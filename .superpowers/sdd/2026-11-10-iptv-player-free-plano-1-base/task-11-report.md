# T11 — Telas de contas e resumo, worker e Release v0.1.0
Status: **DONE** para os passos 1–6 e 9 do brief (únicos que eu executei, por instrução
explícita do orquestrador). **Passos 7 e 8 não executados** — ficam para o orquestrador/dono:
passo 7 exige o "sim" explícito do dono para push + tag `v0.1.0`; passo 8 é validação manual
no celular físico, que eu não tenho como fazer.

Testes do brief (`CargaWorkerTest`) rodaram de verdade: falharam primeiro por erro de
compilação real (`@ExperimentalMaterial3Api`, achado abaixo), passaram depois (2/2). Suíte
completa do projeto: **9 classes, 43 testes, 0 falhas**. `assembleDebug`: `BUILD SUCCESSFUL`,
APK gerado em `app/build/outputs/apk/debug/app-debug.apk`.

Commits feitos: `90db6eb` (`feat: telas de contas e resumo com carga em segundo plano`) e
`3d50dc2` (`docs: skill da API Xtream e da carga`). Este relatório e o ledger do SDD
(`progress.md`) ficam **fora** desses dois commits, por instrução literal do brief — cabe ao
orquestrador decidir onde/quando commitá-los (mesmo padrão do commit dedicado da T10,
`98ce68b`).

## O que foi feito

### Arquivos
- Criado `app/src/main/java/io/github/pagmovel/iptvplayer/AppIptv.kt`: `class AppIptv :
  Application()` com `banco`, `cofre`, `criarApi` e `contasRepositorio` por `lazy`, montados
  à mão (sem DI).
- Criado `app/src/main/java/io/github/pagmovel/iptvplayer/carga/CargaWorker.kt`:
  `class CargaWorker(appContext, params, banco, contasRepositorio, criarApi) :
  CoroutineWorker`, com um construtor secundário `(appContext, params)` que resolve as
  dependências reais a partir do `AppIptv` (ver "Decisões de design" abaixo). `doWork()`
  busca a conta, chama `CargaCatalogo.executar`, reporta `etapa` via `setProgress`, chama
  `marcarCarga` no sucesso e traduz `FalhaXtream` em `mensagem` pt-BR na falha. Companion com
  `nomeTrabalho(contaId)` e `enfileirar(workManager, contaId)` (trabalho único,
  `ExistingWorkPolicy.KEEP`, `Constraints` com `NetworkType.CONNECTED`).
- Criado `app/src/test/java/io/github/pagmovel/iptvplayer/carga/CargaWorkerTest.kt`: os 2
  testes literais do brief, com `TestListenableWorkerBuilder` + uma `WorkerFactory` de teste
  que injeta banco Room em memória, `ContasRepositorio` com `Cofre` falso e `XtreamApi` contra
  `MockWebServer` — sem precisar do `AppIptv` real (que usa `CofreKeystore`/`AndroidKeyStore`,
  não emulado pelo Robolectric).
- Criado `app/src/main/java/io/github/pagmovel/iptvplayer/ui/Tema.kt`: `TemaIptv` — Material 3
  escuro, fundo `#0F1115`, destaque `#4FC3F7`, e os 3 estilos de texto que por padrão ficam
  abaixo de 14sp (`bodySmall`, `labelMedium`, `labelSmall`) subidos para 14sp.
- Criado `app/src/main/java/io/github/pagmovel/iptvplayer/ui/Navegacao.kt`: `Navegacao(app)` —
  troca manual entre `Tela.RESUMO`/`Tela.CONTAS` com `remember { mutableStateOf(...) }`, sem
  `navigation-compose`.
- Criado `app/src/main/java/io/github/pagmovel/iptvplayer/ui/contas/ContasViewModel.kt` e
  `ContasTela.kt`: lista de contas (ativa marcada), formulário com "colar link do provedor"
  (`lerLinkXtream` na área de transferência via `LocalClipboardManager`), ativar, editar
  servidor, apagar com confirmação.
- Criado `app/src/main/java/io/github/pagmovel/iptvplayer/ui/resumo/ResumoViewModel.kt` e
  `ResumoTela.kt`: conta ativa, contagens, etapa da carga (via `WorkInfo` observado por
  `WorkManager.getWorkInfosForUniqueWorkFlow`), data da última carga, "Atualizar agora"; sem
  conta ativa, chama `onPrecisaDeConta` direto.
- Editado `app/src/main/java/io/github/pagmovel/iptvplayer/ui/MainActivity.kt`: troca o
  esqueleto por `TemaIptv { Navegacao(applicationContext as AppIptv) }`.
- Editado `app/src/main/AndroidManifest.xml`: `android:name=".AppIptv"` na `<application>`.
  **Nenhuma permissão nova adicionada** — ver achado sobre `ACCESS_NETWORK_STATE` abaixo.
- Editado `app/src/main/java/io/github/pagmovel/iptvplayer/xtream/FalhaXtream.kt`: nova
  função `fun FalhaXtream.mensagemUsuario(): String`, usada pelo `CargaWorker` **e** por
  `ContasViewModel.salvar` — tradução pt-BR centralizada num único lugar.
- Editado `app/src/main/java/io/github/pagmovel/iptvplayer/conta/ContasRepositorio.kt`: 2
  métodos novos, pequenos e sem teste próprio (um-liners que delegam para operações já
  testadas em T10) — `fun listar(): Flow<List<Conta>>` e `suspend fun editarServidor(conta,
  servidor)`. Precisei deles porque o brief pede "editar (trocar servidor)" e a tela de
  contas lista contas, e nenhuma das duas operações existia ainda em `ContasRepositorio`.
- Editado `app/src/main/java/io/github/pagmovel/iptvplayer/app/build.gradle.kts`:
  `implementation(libs.androidx.work.runtime)` e `testImplementation(libs.androidx.work.testing)`.
- Editado `gradle/libs.versions.toml`: versão nova `workTesting = "2.12.0"` (não reaproveitei
  `workRuntime`, seguindo literalmente o brief) e a lib `androidx-work-testing`.
- Criado `.claude/skills/iptv-xtream-carga/SKILL.md` (passo 9).

### Assinaturas reais confirmadas antes de escrever (lidas do código, não supostas)
- `CargaCatalogo(api: XtreamApi, banco: BancoApp, contaId: Long, agora: () -> ZonedDateTime =
  { ZonedDateTime.now() })` — **o meu brief dizia `agora: () -> Long`; o código real usa
  `() -> ZonedDateTime`** (confirmado lendo `carga/CargaCatalogo.kt` antes de chamar o
  construtor). Não usei o tipo errado porque li o arquivo primeiro.
- `ContasRepositorio(banco, cofre, criarApi)` com `testarESalvar`, `ativar`, `apagar`,
  `credenciais`, `precisaAtualizar` — exatamente como o meu brief descrevia, confirmado linha
  a linha.
- `FalhaXtream` (`xtream/FalhaXtream.kt`): `CredenciaisInvalidas()` sem argumento,
  `ContaInativa(status: String)`, `ServidorIndisponivel(causa: Throwable?)`,
  `RespostaInvalida(detalhe: String)` — os nomes dos campos (`status`, `causa`, `detalhe`) só
  foram usados depois de ler o arquivo; o meu brief não os citava.
- `ContaDao`: `listar(): Flow<List<Conta>>`, `ativa(): Conta?`, `marcarCarga(id, quando)`,
  **sem** método de buscar por id — por isso `CargaWorker.doWork()` usa
  `banco.contaDao().listar().first().firstOrNull { it.id == contaId }` em vez de inventar uma
  query nova.
- `CatalogoDao.contagem(contaId): Contagem` e `data class Contagem(canais, filmes, series,
  programas)` — usados sem mudança em `ResumoViewModel`.
- `BancoApp` não tinha nenhuma função de fábrica de banco de produção (só o `Room
  .inMemoryDatabaseBuilder` dos testes) — confirmei isso lendo `BancoApp.kt` e todos os
  arquivos de teste antes de decidir que precisava criar isso em `AppIptv`, como o brief já
  avisava.

### Decisões de design (onde o brief deixava espaço)

- **`CargaWorker` usa delegação de construtor em vez de `Configuration.Provider` +
  `WorkerFactory` customizada no `Configuration` do app.** O construtor de 5 parâmetros recebe
  as dependências direto; um construtor secundário de 2 parâmetros `(Context,
  WorkerParameters)` — o que o `WorkerFactory` padrão do WorkManager busca por reflexão
  (`getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)`) — resolve as
  dependências reais a partir do `AppIptv`. **Verifiquei isso baixando o código-fonte real de
  `work-runtime:2.12.0` e `work-testing:2.12.0` do Maven do Google nesta sessão** (não
  suposição de "é o padrão do framework"): li `WorkerFactory.kt`
  (`createWorkerWithDefaultFallback` usa exatamente esse `getDeclaredConstructor`),
  `CoroutineWorker.kt` (`doWork()` é `public`, `setProgress` é `suspend` e chama
  `setProgressAsync(data).await()`) e `TestListenableWorkerBuilder.java`/`.kt` (confirma
  `setWorkerFactory`, `setInputData` e o `build()` que chama
  `workerFactory.createWorkerWithDefaultFallback(...)`). Essa verificação evitou escolher
  `Configuration.Provider` (que exigiria manifest extra ou inicialização manual do
  WorkManager) sem necessidade — a delegação de construtor não precisa de nada disso e é
  testável do mesmo jeito com `TestListenableWorkerBuilder.setWorkerFactory`.
- **`ACCESS_NETWORK_STATE` não foi adicionada ao manifest.** O brief pedia para verificar
  isso na documentação antes de assumir. Baixei o `work-runtime-2.12.0.aar` real do Maven do
  Google e extraí o `AndroidManifest.xml` dele: a própria biblioteca já declara
  `ACCESS_NETWORK_STATE` (e `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`), que
  o merge de manifest do AGP traz automaticamente para o app. Deixei um comentário no
  `AndroidManifest.xml` apontando essa verificação.
- **`ContasViewModel`/`ResumoViewModel` não são `androidx.lifecycle.ViewModel`** — são
  guardadores de estado comuns, `remember`ados em `Navegacao`. Evita depender de
  `ViewModelProvider`/`lifecycle-viewmodel-compose` (nenhuma dependência nova) e de decidir se
  o app precisa ou não de uma instância que sobrevive à rotação para duas telas provisórias.
  Marcado com `ponytail:` no código (perde o formulário aberto numa rotação; trocar por
  `androidx.lifecycle.ViewModel` se isso incomodar de verdade).
- **Navegação manual com `enum class Tela` + `remember { mutableStateOf(...) }`**, sem
  `navigation-compose` — o catálogo nem declarava essa dependência, e o app só tem duas telas
  (YAGNI, como o brief pedia explicitamente).
- **`runBlocking` dentro do callback (não-suspend) de `CargaCatalogo.executar` para chamar
  `setProgress` (suspend).** Verifiquei que isso não arrisca deadlock no teste: li
  `TestProgressUpdater.java` (resolve o `ListenableFuture` **sincronamente**, no mesmo
  `completer.set(null)`), então `.await()` nunca suspende de verdade sob
  `TestListenableWorkerBuilder`. Em produção, `CoroutineWorker` roda em `Dispatchers.Default`
  por padrão (confirmado em `CoroutineWorker.kt`) — bloquear essa thread de fundo
  brevemente enquanto o `ProgressUpdater` real grava no banco (outro executor) é aceitável.
  Comentário `ponytail:` no código com o teto e o caminho de troca (progresso suspend).
- **`ContasRepositorio.editarServidor` não testa a credencial de novo** antes de salvar —
  diferente de `testarESalvar`. O brief só pedia "editar (trocar servidor)", sem reautenticar;
  se o endereço novo estiver errado, a próxima carga reporta isso como falha normal
  (`ServidorIndisponivel`/etc., já traduzida).
- **Mensagem "Aguardando conexão com a internet" vem do próprio `WorkInfo.State.ENQUEUED`**,
  não de checar `ConnectivityManager` manualmente — a constraint `NetworkType.CONNECTED` já
  impede o worker de rodar sem rede, e esse estado parado é o sinal nativo do WorkManager para
  "esperando a constraint".

## Verificação

### Passo 1 (testes escritos) — feito
`CargaWorkerTest.kt`, os 2 testes literais do brief, escritos antes de qualquer ajuste de UI
posterior.

### Passo 2 (rodar e ver falhar) — feito, falha real de compilação
```
./scripts/gradle.ps1 testDebugUnitTest --tests '*CargaWorkerTest'
```
Primeira rodada: `:app:compileDebugKotlin FAILED` —
`ui/contas/ContasTela.kt:45:13 This material API is experimental and is likely to change or
to be removed in the future.` (`TopAppBar` do Material 3 exige `@OptIn(ExperimentalMaterial3Api::class)`
nesta versão do catálogo). Log salvo em `temp/t11-worker-test.log` (fora do git). Corrigi com
`@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)` no topo do arquivo.

### Passo 3 (implementar o worker) — feito, conforme acima.

### Passo 4 (implementar as telas) — feito, conforme acima.

### Passo 5 (rodar tudo) — feito, passagem real
```
./scripts/gradle.ps1 testDebugUnitTest --tests '*CargaWorkerTest'   # depois do fix: BUILD SUCCESSFUL, 2/2
./scripts/gradle.ps1 testDebugUnitTest assembleDebug                 # BUILD SUCCESSFUL in 6m 5s
```
Confirmado não só pela saída do Gradle, mas lendo os XML do JUnit em
`app/build/test-results/testDebugUnitTest/`: **9 classes, 43 testes, 0 falhas** (8
classes/41 testes antes da T11 + `CargaWorkerTest` com 2 testes). `assembleDebug` gerou
`app/build/outputs/apk/debug/app-debug.apk`.

### Passo 6 (commit) — feito
`90db6eb` — `feat: telas de contas e resumo com carga em segundo plano`, autor/committer
`Marcos Ronaldo <marcos.ronaldo@gmail.com>` (4 variáveis de ambiente, sem tocar `git config`
— confirmei `git config --local user.name`/`user.email` vazios antes do commit). Commitei só
os arquivos da T11; deixei de fora `.claude/` (foi para o commit do passo 9, separado, como o
brief pedia) e `fwcd.kotlin-0.2.36.vsix` (untracked, pré-existente, não relacionado — **ainda
sem resposta do dono sobre comitar ou não**, ver "Pendência" abaixo).

### Passo 9 (skill) — feito
`.claude/skills/iptv-xtream-carga/SKILL.md`, com o formato real da API (player_api.php,
xmltv.php), as 6 pegadinhas confirmadas (incluindo as 4 citadas no meu brief: temporadas
vazias em `get_series_info`, `get.php` redirecionando pra `www.google.com`, tipos
inconsistentes no JSON, ids de EPG repetidos no XMLTV — as 4 vieram do design §3.1 e do
código real de `XtreamApiTest`/`JsonCampos.kt`, não inventadas) e os acertos (streaming em
lotes, duas transações separadas, tradução de erro centralizada, infra do Robolectric).
Commit `3d50dc2` — `docs: skill da API Xtream e da carga`.

**Não atualizei** as tabelas de acompanhamento do plano nem a §6 do design — por instrução
explícita do orquestrador, que faz isso por fora desta tarefa.

## Autorrevisão dos 2 cenários de teste contra a lógica

1. **`worker grava o catalogo e marca a carga`**: enfileira as 8 respostas na ordem exata das
   8 chamadas HTTP que `CargaCatalogo.executar` faz (auth + 3 categorias + canais + filmes +
   séries + xmltv, todas vazias/`[]`/`<tv></tv>`), chama `criarWorker().doWork()` direto (sem
   `startWork()` — confirmei que `doWork()` é `public` lendo `CoroutineWorker.kt`) e confere
   `Result.Success` + `ultimaCargaEm != null` na conta recarregada do banco. Não testei as
   contagens aqui porque já são cobertas por `CargaCatalogoTest` — este teste é sobre o
   worker (tradução pra `Result`/`marcarCarga`), não sobre a carga em si.
2. **`worker traduz falha em mensagem`**: só 1 resposta enfileirada (`auth: 0`) — a falha
   acontece na autenticação, antes de qualquer outra chamada, então não precisa enfileirar
   mais nada. Confere `Result.Failure` e `outputData.getString("mensagem") ==
   "Usuário ou senha incorretos"`, usando a função `mensagemUsuario()` compartilhada com a
   tela de contas (ver "Decisões de design").

## YAGNI / ponytail

- Não criei `androidx.lifecycle.ViewModel`, `ViewModelProvider`/factory, nem dependência de
  `lifecycle-viewmodel-compose` — guardadores de estado comuns bastam para 2 telas
  provisórias (comentário `ponytail:` com o teto).
- Não criei `navigation-compose` — `enum class Tela` + `remember` resolve 2 telas sem
  rotas/back stack de biblioteca.
- Não criei `Configuration.Provider`/`WorkerFactory` customizada no `Configuration` do app —
  delegação de construtor no próprio `CargaWorker` resolve a injeção sem mexer no
  `Application`/manifest além do `android:name`.
- Não reautentico a credencial em `editarServidor` — o brief só pedia trocar o servidor; a
  próxima carga já reporta falha se o endereço estiver errado.
- `runBlocking` dentro do callback de progresso — marcado com `ponytail:`, teto e troca
  (progresso suspend) documentados no código, não só aqui.
- Os 2 métodos novos de `ContasRepositorio` (`listar`, `editarServidor`) são um-liners que
  delegam para operações já testadas em T10 — não escrevi teste novo pra eles (regra do
  projeto: um-liner trivial não precisa de teste próprio).

## Pendência para o orquestrador/dono

- **`fwcd.kotlin-0.2.36.vsix`** (untracked, raiz do projeto) continua sem ir para nenhum
  commit meu — não é meu arquivo, e a regra do projeto é perguntar antes de commitar algo que
  não criei. Preciso que o dono diga se entra no repositório ou não.
- Passos 7 (push + tag `v0.1.0`) e 8 (validação no celular) ficam para o dono, como o brief
  determinou.
- Relatório da T9 (`CargaCatalogo`) usa `agora: () -> Long` na descrição do brief original;
  o código real usa `() -> ZonedDateTime` — já estava assim antes desta tarefa (T9/T10 não
  mudaram isso), só registro aqui porque usei a assinatura real, não a do texto do brief.

## Dúvidas (nenhuma bloqueante — só registro)

- A única decisão genuinamente aberta pelo brief foi como o `CargaWorker` obtém suas
  dependências em produção sem framework de DI — resolvida com delegação de construtor,
  verificada contra o código-fonte real do WorkManager (não suposição), não pergunta.
- `ACCESS_NETWORK_STATE` era uma pergunta explícita do brief ("verifique... antes de supor")
  — resolvida extraindo o manifest real do AAR, resposta: não precisa declarar.
