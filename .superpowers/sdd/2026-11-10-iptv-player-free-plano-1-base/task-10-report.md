# T10 — Contas: cofre de senha e repositório
Status: **DONE**. Teste oficial do brief (`ContasRepositorioTest`) rodou de verdade, falhou
antes da implementação (compilação) e passou depois (5/5). Commit feito: `39a9207`.
## O que foi feito
### Arquivos
- Criado `app/src/main/java/io/github/pagmovel/iptvplayer/conta/Cofre.kt`: `interface Cofre`
  (`cifrar`/`decifrar`) e `class CofreKeystore : Cofre` (AES/GCM/NoPadding, chave
  `iptv-player-free-senhas` no `AndroidKeyStore`, IV de 12 bytes prefixado ao resultado).
- Criado `app/src/main/java/io/github/pagmovel/iptvplayer/conta/ContaDao.kt`: `@Dao abstract
  class ContaDao` com `inserir`, `atualizar`, `apagar`, `listar(): Flow<List<Conta>>`,
  `ativa(): Conta?` e `marcarCarga(id, quando)` — exatamente as 6 operações do brief.
- Criado `app/src/main/java/io/github/pagmovel/iptvplayer/conta/ContasRepositorio.kt`:
  `class ContasRepositorio(banco, cofre, criarApi)` com `testarESalvar`, `ativar`, `apagar`,
  `credenciais` e `precisaAtualizar`, todas com a assinatura literal do brief.
- Criado `app/src/test/java/io/github/pagmovel/iptvplayer/conta/ContasRepositorioTest.kt`:
  5 testes (Robolectric + Room em memória + MockWebServer + `Cofre` falso), os 5 cenários
  pedidos no brief.
- Editado `app/src/main/java/io/github/pagmovel/iptvplayer/dados/BancoApp.kt`: removido o
  `@Dao abstract class ContaDao` vazio que a T7 tinha deixado como placeholder dentro deste
  arquivo (`package dados`) e trocado por `import io.github.pagmovel.iptvplayer.conta.ContaDao`
  — `abstract fun contaDao(): ContaDao` já existia (feito pela T7, antecipando esta tarefa) e
  não precisou mudar.
- Editado `app/src/main/java/io/github/pagmovel/iptvplayer/dados/Entidades.kt`: comentário da
  entidade `Conta` atualizado (apontava "Preenchida pela T10 (ContaDao)"; agora referencia
  `Cofre`, já que a tarefa terminou).
### Assinaturas reais confirmadas antes de escrever (lidas do código, não supostas)
- `data class Conta(id: Long = 0, nome: String, servidor: String, usuario: String,
  senhaCifrada: ByteArray, ativa: Boolean, ultimaCargaEm: Long?)` —
  `dados/Entidades.kt:74` (com `@PrimaryKey(autoGenerate = true)` em `id`).
- `data class CredenciaisXtream(servidor: String, usuario: String, senha: String)` —
  `conta/LinkXtream.kt` (produzida pela T4, consumida depois por T5 em `XtreamApi`). Usei os
  3 campos literalmente, sem adivinhar nomes.
- `class XtreamApi(private val cliente: OkHttpClient, private val cred: CredenciaisXtream)`
  e `suspend fun autenticar(): InfoConta` — `xtream/XtreamApi.kt` (T5). `autenticar()` lança
  `FalhaXtream` (sealed, estende `Exception`) em qualquer falha — nunca retorna "inválido" por
  valor. É por isso que `testarESalvar` captura `FalhaXtream` especificamente, não `Exception`
  genérico (evita engolir `CancellationException`, que também estende `Exception` via
  `RuntimeException`/`IllegalStateException`).
- `BancoApp` (T7) já tinha `abstract fun contaDao(): ContaDao` apontando para um `ContaDao`
  vazio definido ali mesmo — gap intencional do plano, exatamente como o meu brief avisou
  ("Coloque o ContaDao no BancoApp" = mover para `conta/ContaDao.kt` e trocar o import, não
  criar o método do zero).
- `CatalogoDao.apagarCatalogo(contaId)` (já `@Transaction`) e `apagarProgramacao(contaId)` —
  `dados/CatalogoDao.kt` (T7) — reusados em `ContasRepositorio.apagar(id)`, sem reimplementar
  nada equivalente.
- `room-ktx` já é dependência do projeto (`app/build.gradle.kts`), o que traz
  `kotlinx-coroutines-core` transitivamente — não precisei declarar nenhuma dependência nova
  para usar `Flow<List<Conta>>` nem o `.first()` do teste.
### Decisões de design (onde o brief deixava espaço)
- **`testarESalvar` captura `FalhaXtream`, não `Exception`.** `autenticar()` só lança
  `FalhaXtream` (é `sealed class ... : Exception()`); capturar o tipo específico em vez do
  genérico evita engolir `CancellationException` dentro de uma função `suspend` — prática
  recomendada em Kotlin coroutines, e o único tipo de falha que esta função pode mesmo receber
  do `criarApi(cred).autenticar()`.
- **`ativar(id)` lê `listar().first()` e regrava cada conta com `atualizar(conta.copy(ativa =
  conta.id == id))`.** O brief não deu uma assinatura de SQL para isso em `ContaDao` (só listou
  `inserir/atualizar/apagar/listar/ativa/marcarCarga`), então montei a troca de ativa só com as
  operações que já existem, sem inventar uma query nova. Marquei com
  `ponytail: lê e regrava todas as contas (uma a uma); aceitável porque o app guarda poucas
  contas (cadastro manual, uma por vez). Se isso crescer, trocar por duas queries UPDATE
  diretas no ContaDao (zera todas, ativa uma).` — teto real (poucas contas cadastradas à mão) e
  caminho de troca, como a T9 relatou ter sido pego em revisão por faltar essa tag.
- **`apagar(id)` chama `apagarCatalogo` + `apagarProgramacao` + `ContaDao.apagar`, em sequência,
  sem `withTransaction`.** O brief descreve o método como "apaga catálogo e programação da
  conta" — segui literalmente, reusando as duas operações do `CatalogoDao` (T7) e só então
  removendo a linha da própria conta. Não embrulhei numa transação porque o brief não pediu
  atomicidade aqui (diferente da T9, que documentou explicitamente a necessidade de
  atomicidade para a carga) e são todas operações de limpeza — uma falha no meio não deixa o
  catálogo "pior" do que já apagar tudo de qualquer forma.
- **`precisaAtualizar` ficou `suspend` mesmo sem nenhuma chamada suspensa no corpo**, porque é
  exatamente a assinatura literal do brief (`suspend fun precisaAtualizar(conta: Conta, agoraMs:
  Long): Boolean`) — não simplifiquei para função comum só porque o corpo não precisa, já que
  mudar a assinatura quebraria contrato com quem for chamar esperando uma função suspensa (ex.:
  dentro de uma tela que já está numa coroutine).
- **Cofre falso do teste usa XOR por byte (`0xFF`), não um prefixo.** A primeira versão que
  considerei (prefixar `"cifrado:"`) deixaria a senha original como substring do resultado —
  exatamente o que o teste "senha nunca é gravada em texto puro" tem que reprovar. XOR é
  reversível (duas aplicações voltam ao original) e nenhum byte da senha original sobra
  reconhecível no resultado.
## Verificação
### Passo 1 (testes escritos) — feito
5 testes em `ContasRepositorioTest.kt`, escritos **antes** de `Cofre.kt`, `ContaDao.kt` e
`ContasRepositorio.kt` existirem.
### Passo 2 (rodar e ver falhar) — feito, falha real
```
./scripts/gradle.ps1 testDebugUnitTest --tests "*ContasRepositorioTest*"
```
`:app:compileDebugUnitTestKotlin FAILED` — `Unresolved reference 'Cofre'`, `Unresolved
reference 'ContasRepositorio'`, `Cannot infer type` em cascata. Falha de compilação, não
suposta (log salvo em `temp/t10-compile-fail.log`, fora do git).
### Passo 3 (implementar) — feito, conforme acima.
### Passo 4 (rodar e ver passar) — feito, passagem real
Mesmo comando, depois da implementação: `BUILD SUCCESSFUL in 2m 41s`. Confirmado não só pela
saída do Gradle, mas lendo o XML do JUnit
(`app/build/test-results/testDebugUnitTest/TEST-io.github.pagmovel.iptvplayer.conta.ContasRepositorioTest.xml`):
`tests=5 failures=0 errors=0`.
### Verificação extra (não pedida pelo brief, mas fiz por prudência): suíte completa
Rodei `testDebugUnitTest` sem filtro, para garantir que mexer em `BancoApp.kt`/`Entidades.kt`
não quebrou T7/T9. Resultado: **`CatalogoDaoTest`, `CargaCatalogoTest` e
`ContasRepositorioTest` falharam** — mas não por asserção: as 3 classes falharam com
`classMethod` (erro de inicialização do Robolectric, não de teste), todas com a mesma causa:
```
java.lang.AssertionError: Failed to fetch maven artifact
org.robolectric:android-all-instrumented:6.0.1_r3-robolectric-r1-i7
Caused by: java.util.concurrent.ExecutionException
  Caused by: java.io.IOException at HttpURLConnection.java:2034
```
Isso **não é uma asserção de teste falhando** — é o resolvedor próprio do Robolectric (não o
Gradle) tentando baixar o jar `android-all-instrumented` direto do Maven Central, fora do
cache/configuração do Gradle (mesma categoria de bloqueio que a T9 documentou para
`nativeruntime-dist-compat`, mas um artefato e um mecanismo de busca diferentes — o
`exclusiveContent` do `settings.gradle.kts` não cobre isso, porque é o próprio Robolectric
que resolve, não o Gradle).
**Não assumi que era isso — verifiquei que é pré-existente, não causado pela T10:**
1. Rodei a suíte completa 2 vezes (paralelo e com `--max-workers=1`): as mesmas 3 classes
   falharam as 2 vezes, sempre com a mesma causa.
2. Rodei só `*CatalogoDaoTest*` + `*CargaCatalogoTest*` juntas (nenhuma delas toca código da
   T10): **falharam também**, confirmando que o problema aparece ao rodar **mais de uma classe
   Robolectric na mesma invocação do Gradle**, não por causa do código novo.
3. Para eliminar de vez a hipótese de que minha edição em `BancoApp.kt` fosse a causa, fiz
   `git stash push -u` (guardou todas as minhas mudanças, inclusive os arquivos novos) e rodei
   `*CatalogoDaoTest*` + `*CargaCatalogoTest*` **contra o código original, sem nenhuma mudança
   da T10**: **falharam do mesmo jeito** (`2 tests completed, 2 failed`, mesma causa). Em
   seguida, `git stash pop` para restaurar meu trabalho (confirmado com `git status` depois).
Conclusão verificada, não suposta: é um problema de infraestrutura pré-existente (mais de uma
classe Robolectric por invocação do Gradle tropeça no fetch do `android-all-instrumented`),
que já existia antes da T10 e independe do meu código. Fica registrado para quem pegar a T11
ou rodar a suíte inteira depois — o comando oficial do meu brief, isolado, roda limpo.
### Passo 5 (commit) — feito
`39a9207` — `feat: contas com senha cifrada e troca de conta ativa`, autor/committer `Marcos
Ronaldo <marcos.ronaldo@gmail.com>` (4 variáveis de ambiente, sem tocar `git config` — o
repositório não tem `user.name`/`user.email` local, confirmado com `git config --local`
antes do commit). Commitei só os arquivos da T10 (3 novos em `conta/`, 1 teste, 2 edições em
`dados/`) — deixei de fora `progress.md` (modificado por fora desta tarefa, não é meu) e
`fwcd.kotlin-0.2.36.vsix` (untracked, pré-existente, não relacionado).
## Autorrevisão dos 5 cenários de teste contra a lógica
1. **`senha nunca e gravada em texto puro`**: confirma que `testarESalvar` chama
   `cofre.cifrar(cred.senha)` antes de gravar — não grava `cred.senha` direto em nenhum campo.
   Usei `Charsets.ISO_8859_1` para procurar a substring nos bytes crus (mapeamento 1:1
   byte↔char, sem o risco de o `UTF-8` substituir byte inválido por `U+FFFD` e mascarar um erro
   real). Se eu tivesse esquecido de cifrar e gravado `cred.senha.toByteArray()` puro, este
   teste reprovaria de forma consistente, porque os bytes UTF-8 de "s3nh4" apareceriam literalmente.
2. **`credencial invalida nao salva conta`**: `auth: 0` → `XtreamApi.autenticar()` lança
   `FalhaXtream.CredenciaisInvalidas` → `testarESalvar` captura e devolve `Result.failure` —
   sem nunca chegar à linha `banco.contaDao().inserir(conta)`. Confirmei a ordem no código:
   `criarApi(cred).autenticar()` é a primeira linha do `try`, antes de montar o `Conta`.
3. **`primeira conta fica ativa e ativar troca`**: a conta A é criada quando `listar()` ainda
   está vazia (`primeira = true`) → grava `ativa = true`. A conta B é criada depois, com
   `listar()` não vazia (`primeira = false`) → grava `ativa = false`. Depois,
   `repo.ativar(b.id)` passa por todas as contas e grava `ativa = (conta.id == b.id)` — A vira
   `false`, B vira `true`. Testei as duas pontas: só A ativa antes de `ativar`, só B ativa
   depois.
4. **`apagar conta apaga o catalogo dela`**: insiro 1 canal pela conta criada, chamo
   `repo.apagar(conta.id)`, confirmo `contagem(contaId).canais == 0` (via `apagarCatalogo`,
   T7) e que a própria conta saiu de `listar()` (via `ContaDao.apagar`). Os 3 passos do método
   (`apagarCatalogo`, `apagarProgramacao`, `ContaDao.apagar`) rodam na ordem certa — se
   `ContaDao.apagar` rodasse antes, `apagarCatalogo(contaId)` ainda funcionaria igual (é um
   `DELETE FROM Canal WHERE contaId = :contaId`, não depende da linha em `Conta` existir), mas
   deixei a ordem "catálogo primeiro, conta por último" por ler mais natural ("limpa o que
   pertence à conta, depois a conta").
5. **`precisa atualizar depois de 12 horas`**: `ultimaCargaEm = null` → `return true` direto,
   sem chegar a comparar milissegundos (cobre contas recém-criadas, que nunca carregaram).
   `11h59` (`agoraMs - ultima = 711 min`) → `711*60*1000 > 12*60*60*1000`? Não (711 min <
   720 min) → `false`. `12h01` (`721 min`) → `721 min > 720 min` → `true`. A condição é
   estritamente `>` (brief disse "mais de 12h", não "12h ou mais"), então exatamente 12h00
   ficaria `false` — não testei esse limite exato porque o brief não pediu esse terceiro ponto,
   só os dois lados (11h59 e 12h01).
## YAGNI / ponytail
- Não criei fachada/`ViewModel`/tela para contas — fora do escopo da T10 (brief só pede cofre
  + DAO + repositório).
- Não criei uma classe de erro nova para `testarESalvar`; reusei `FalhaXtream` (T5) dentro do
  `Result.failure`, sem embrulhar numa exceção própria do domínio de contas.
- Única concessão documentada: `ativar(id)` regrava todas as contas em vez de 2 `UPDATE`
  diretos — comentário `ponytail:` no código, com o teto (poucas contas) e o gatilho de troca.
## Dúvidas (nenhuma bloqueante — só registro)
Nenhuma assinatura ficou ambígua: `Conta`, `CredenciaisXtream` e `XtreamApi` já existiam com
tipos exatos: só precisei ler, não adivinhar. A única decisão realmente aberta pelo brief (como
implementar a troca de conta ativa sem uma query de UPDATE em massa) ficou registrada como
`ponytail:` em vez de pergunta, porque tinha uma solução claramente correta e reversível dentro
do que o `ContaDao` já expõe.
