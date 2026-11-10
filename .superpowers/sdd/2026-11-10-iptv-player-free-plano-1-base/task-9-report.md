# T9 — Carga do catálogo (orquestração)

Status: **DONE_WITH_CONCERNS** (código e testes implementados conforme o brief; verificação
dinâmica da suíte **bloqueada** por um 429 do Maven Central — confirmado duas vezes, não é bug
meu). Compilação real confirmada duas vezes. **Sem commit** (regra do brief: só commitar com
verificação real; não tive passo 4 — "ver passar" — completo).

## O que foi feito

### Arquivos

- Criado `app/src/main/java/io/github/pagmovel/iptvplayer/carga/CargaCatalogo.kt`:
  `enum class EtapaCarga` e `class CargaCatalogo(api, banco, contaId, agora)` com
  `suspend fun executar(progresso): Contagem`, exatamente com as assinaturas do brief.
- Criado `app/src/test/java/io/github/pagmovel/iptvplayer/carga/CargaCatalogoTest.kt`: 4 testes
  (Robolectric + Room em memória + MockWebServer), os 4 cenários pedidos no brief.

### Assinaturas reais confirmadas antes de escrever (lidas do código, não supostas)

- `XtreamApi.autenticar()`, `lerLista(acao, tamanhoLote=500, aoLote: suspend (List<JsonObject>)->Unit)`,
  `abrirXmltv(ler: suspend (InputStream)->T): T` — `xtream/XtreamApi.kt`.
- `JsonObject.paraCategoria(contaId, tipo)`, `paraCanal(contaId)`, `paraFilme(contaId)`,
  `paraSerie(contaId)` (todas `?`, descartam o item se faltar id/nome) — `xtream/Mapeamento.kt`.
- `CatalogoDao.inserirCategorias/Canais/Filmes/Series/Programas`, `apagarCatalogo(contaId)`
  (já `@Transaction`), `apagarProgramacao(contaId)`, `contagem(contaId): Contagem` — `dados/CatalogoDao.kt`.
- `lerXmltv(entrada, contaId, janela, tamanhoLote=500, aoLote: (List<Programa>)->Unit): Int`
  (bloqueante, **não** suspend) e `janelaProgramacao(agora): LongRange` — `programacao/LeitorXmltv.kt`.
- `androidx.room.withTransaction` (room-ktx) confirmado como dependência já declarada em
  `app/build.gradle.kts` (`implementation(libs.androidx.room.ktx)`), não usado em nenhum outro
  arquivo ainda — primeiro uso real é este.
- Nomes das 6 ações Xtream (`get_live_categories`, `get_vod_categories`, `get_series_categories`,
  `get_live_streams`, `get_vod_streams`, `get_series`) confirmados em `temp/coletar_amostras.py`
  (script de levantamento do dono, fora do git) — não inventados, e não copiei dados de
  `temp/amostras/`, só os nomes das ações.

### Ordem implementada (confere com o brief)

1. `progresso(AUTENTICANDO)` → `api.autenticar()` — fora de qualquer transação, então senha
   errada não toca o banco.
2. `banco.withTransaction` #1: `dao.apagarCatalogo(contaId)` (já é `@Transaction`, mas Room
   aceita `@Transaction` dentro de `withTransaction` — mesma transação, sem savepoint extra
   visível) → `progresso(CATEGORIAS)` + as 3 categorias (ao vivo/filme/série) → `progresso(CANAIS)`
   + canais → `progresso(FILMES)` + filmes → `progresso(SERIES)` + séries. Qualquer falha aqui
   desfaz a transação inteira (delete + tudo que já tinha sido inserido nela).
3. `progresso(PROGRAMACAO)` → **segunda transação, separada da primeira** (`banco.withTransaction`
   #2): `apagarProgramacao` + ler o XMLTV e inserir os lotes. Falha aqui desfaz só essa
   transação — o catálogo da transação #1, se já tiver comitado, fica de pé.
4. `progresso(CONCLUIDA)` → `dao.contagem(contaId)`.

### Decisão de design que mudei no meio do caminho (documentando por quê)

`lerXmltv` entrega os lotes por um callback **não-suspend** (`(List<Programa>) -> Unit`), mas
`dao.inserirProgramas` é `suspend`. Minha primeira versão usava `runBlocking(coroutineContext)`
capturado de dentro do `withTransaction` para "voltar" à transação a partir do callback
bloqueante. Decidi trocar porque a correção dessa ponte depende de um detalhe interno do Room
(como uma chamada `suspend` feita de dentro de um `runBlocking` aninhado se liga — ou não — à
conexão/thread que o Room reservou para a transação em andamento) que eu não consegui confirmar
lendo código (não achei a fonte do `room-ktx` em cache, só o `.jar`/AAR) nem rodando o teste 4
(justamente o que depende disso: falha na programação não pode deixar lote parcial).

Troquei para algo que eu **consigo** justificar sem depender de internals não verificados: o
callback de `lerXmltv` só acumula os lotes numa lista local (`mutableListOf<List<Programa>>`,
nada de I/O), e um loop comum depois — ainda dentro do mesmo `withTransaction` suspend, no mesmo
empilhamento de corrotina de sempre — chama `dao.inserirProgramas(lote)` para cada um. É
exatamente o mesmo padrão (chamada `suspend` direta de dentro do `withTransaction`) que já uso
para canais/filmes/séries, e que o `CatalogoDaoTest` (T7) já comprovou funcionar com Robolectric.
Custo: os programas da janela (não o XML inteiro de 8,2 MB — isso continua em fluxo via
`XmlPullParser`) ficam em memória entre o fim do parse e o início da gravação, em vez de serem
gravados lote a lote durante a leitura. Para a janela de 4 dias do design isso é na pior das
hipóteses alguns milhares de objetos `Programa` pequenos — aceitável.
`ponytail: lotes acumulados em memória antes de gravar; se a janela crescer (ex. 30 dias) e isso
pesar, trocar por uma fila/canal que empurra para uma corrotina gravadora.`

## Verificação

### Passo 1 (testes escritos) — feito

4 testes em `CargaCatalogoTest.kt`: carga completa (nomes/contagem/ordem das etapas), falha nos
filmes mantém catálogo anterior, senha errada não apaga nada, falha só na programação mantém
catálogo novo e programação antiga.

### Passo 2 (rodar e ver falhar) / Passo 4 (rodar e ver passar) — BLOQUEADO, não fingido

Rodei (duas vezes, com a reescrita no meio) `./scripts/gradle.ps1 testDebugUnitTest --tests
'*CargaCatalogoTest'`. As duas vezes, o Gradle **nem chega a executar o teste**: falha ao
resolver `:app:debugUnitTestRuntimeClasspath`, antes de qualquer teste rodar:

```
Could not resolve all files for configuration ':app:debugUnitTestRuntimeClasspath'.
  > Failed to transform nativeruntime-dist-compat-1.0.19.jar (org.robolectric:nativeruntime-dist-compat:1.0.19) ...
     > Could not download nativeruntime-dist-compat-1.0.19.jar (org.robolectric:nativeruntime-dist-compat:1.0.19)
        > Could not get resource '.../org/robolectric/nativeruntime-dist-compat/1.0.19/nativeruntime-dist-compat-1.0.19.jar'.
           > Received status code 429 from server: Too Many Requests
```

Exatamente o bloqueio avisado no meu brief. Fiz verificação adicional, não só aceitei o aviso:

- **Confirmei o 429 fora do Docker**, direto da máquina Windows, em dois hosts (mesmo resultado
  nos dois, então não é problema de rota do container): `repo.maven.apache.org` → 429;
  `repo1.maven.org` → 429.
- **Tentei o workaround do brief (classpath manual)** antes de desistir: inspecionei o volume
  `iptv-gradle-cache` (`docker run ... find .../modules-2/files-2.1`). Achados:
  - `nativeruntime-dist-compat`: só o `.pom` está em cache, o `.jar` nunca baixou.
  - **Faltam também** `bytebuddy`/`byte-buddy` e `objenesis` (exigidos pelo Robolectric para
    instrumentar as classes shadow) e **qualquer artefato `android-all`/`android-all-instrumented`**
    (o jar do próprio Android que o `RobolectricTestRunner` baixa por conta própria, pelo seu
    resolvedor interno — achei `org.robolectric:plugins-maven-dependency-resolver` em cache, que
    é exatamente esse resolvedor — e não pelo Gradle, então nem aparece na resolução normal de
    dependências). Sem esse jar o Robolectric não sobe sandbox nenhum.
  - Não existe outro volume Docker com cache `.m2` nesta máquina (`docker volume ls`).
- Concluí que montar um classpath manual e rodar com `java -cp ... JUnitCore` não teria como
  funcionar hoje: faltam peças essenciais (bytebuddy, objenesis, o jar do Android) que exigiriam
  exatamente os mesmos downloads bloqueados — e, no caso do `android-all`, um download que o
  Robolectric faz por conta própria, fora do controle do Gradle, no mesmo Maven Central bloqueado.
  Isso bate com o aviso do brief: "esta tarefa é mais dependente de Robolectric que as
  anteriores... o workaround pode não servir aqui". Não gastei tempo montando os ~50 jars à mão
  para confirmar isso na prática, porque a checagem de rede (429 em dois mirrors) e de cache (as
  peças essenciais simplesmente não existem localmente) já é evidência suficiente de que o
  resultado seria o mesmo.

**O que *foi* verificado de verdade, não suposto:** compilei (`compileDebugUnitTestKotlin`, que
roda `kspDebugKotlin` + `compileDebugKotlin` do `main` e depois compila o `test`) com sucesso
duas vezes — a primeira com a versão `runBlocking`, a segunda já com a versão reescrita (loop
simples). As duas compilaram sem erro contra as assinaturas reais de T5–T8 (nenhuma suposta).
Isso confirma tipos, nulabilidade e chamadas corretas — não confirma o comportamento em tempo de
execução (Passo 2/4), que é o que o bloqueio do Maven impede.

### Passo 5 (commit) — não feito

Não commitei. O brief condiciona o commit a ter conseguido verificar, e eu não vi a suíte
passar (nem falhar da forma esperada no passo 2) — só vi a resolução de dependências falhar por
causa externa. Assim que o bloqueio no Maven Central passar (o aviso diz ~23h a partir de
2026-11-10 18:48), rodar de novo:

```powershell
./scripts/gradle.ps1 testDebugUnitTest --tests '*CargaCatalogoTest'
```

Se passar, commitar com a mensagem do brief:
`feat: carga do catálogo em transação, sem perder dados em falha`.

## Autorrevisão

- **Ordem dos `progresso(...)`**: AUTENTICANDO, CATEGORIAS, CANAIS, FILMES, SERIES, PROGRAMACAO,
  CONCLUIDA — bate com a ordem do enum e com a ordem das chamadas HTTP (confirmei linha a linha
  lendo o arquivo final). O teste 1 compara com `EtapaCarga.entries` direto, em vez de repetir a
  lista à mão (evita o teste ficar desatualizado se a ordem do enum mudar sem eu notar).
- **4 cenários do brief, revisados um a um:**
  1. *Carga completa*: 8 respostas enfileiradas na ordem exata das 8 chamadas HTTP que o código
     faz (auth + 3 categorias + canais + filmes + séries + xmltv). Contagem esperada
     `Contagem(3, 2, 1, 4)` bate com os dados enfileirados.
  2. *Falha nos filmes mantém catálogo anterior*: 1ª carga grava 3 canais fora de qualquer
     transação com falha; 2ª carga falha no `get_vod_streams` (503) **dentro** da transação do
     catálogo, depois de já ter rodado `apagarCatalogo` + reinserido categorias/canal(9) nessa
     mesma transação — o rollback do Room desfaz tudo isso de uma vez, então a asserção
     (`canais == 3`, o valor da 1ª carga) só passa se a transação for atômica de ponta a ponta,
     não só "não grava os filmes".
  3. *Senha errada não apaga nada*: a falha de auth acontece **antes** de qualquer
     `withTransaction`/`apagarCatalogo` — não é "a transação desfez", é "a transação nunca
     começou". É a única verificação que não depende do Room, só da ordem autenticar-antes-de-
     mexer-no-banco.
  4. *Falha só na programação mantém catálogo novo e programação antiga*: a transação do
     catálogo (canais 2→3) comita antes da transação da programação começar; a transação da
     programação falha no `abrirXmltv` (503) **depois** de já ter rodado `apagarProgramacao`
     nessa mesma transação, mas **antes** de inserir qualquer lote novo — é exatamente esse
     ponto que exige que o `apagarProgramacao` seja desfeito pelo rollback, não só "não inseriu
     os novos". Essa é a razão de ter trocado o `runBlocking` pela versão com loop simples: eu
     queria ter certeza, sem depender de internals do Room que eu não consegui ler, de que o
     `apagarProgramacao` e os inserts (quando chegam a rodar) estão todos na mesma transação.
- **YAGNI/ponytail**: não criei repositório/fachada extra, não criei uma classe de erro nova
  (reaproveito `FalhaXtream` que já existe), não criei um "gerenciador de progresso" — é só o
  parâmetro `progresso` que o brief pediu. A única concessão documentada é o buffer em memória
  da programação (comentário `ponytail:` no código, com o gatilho de quando reconsiderar).
- **Falta cobrir** (fora do escopo do brief, não adicionei): categorias não entram em `Contagem`
  (o `data class` de T7 não tem esse campo) — então nenhum teste verifica o conteúdo gravado das
  categorias, só que a chamada não quebra. Não inventei um jeito de testar isso porque T7 não
  expõe uma consulta para isso e o brief não pediu.

## Dúvidas (nenhuma bloqueante — só registro)

Nenhuma dúvida de requisito ficou pendente: as 4 assinaturas (T5–T8) e os 4 cenários de teste
estavam explícitos no brief ou no código real. A única incerteza genuína era a semântica interna
do Room para a ponte `runBlocking`, e resolvi evitando depender dela, não perguntando.
