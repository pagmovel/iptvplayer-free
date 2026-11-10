# Relatório — T1: Esqueleto Gradle e testes no Docker

Status: **DONE**

## O que foi feito

Segui os 8 passos do brief, em ordem, com TDD real (teste vermelho → implementação →
teste verde).

### Passo 1 — Conferir a imagem

`docker run --rm cimg/android:2026.10 bash -lc "java -version; sdkmanager --list_installed; gradle --version; id"`
(real, não presumido):

- JDK: OpenJDK **21.0.12.1**.
- Usuário/home: **circleci**, **/home/circleci** — confirma o `[A VERIFICAR]` do brief.
- `gradle` **9.8.2** já está no PATH da imagem.
- Platforms instaladas: `android-34, 35, 36, 36.1, 37.0, 37.1, 37.2`.
- Conferido o `source.properties` de `android-37.2`: `AndroidVersion.ApiLevel=37.2`,
  `CodeName` vazio, `PreviewSdkInt=0` → é estável, não preview. Maior NN = **37** (minor
  mais recente: **2**).

### Passo 2 — `scripts/gradle.ps1`

Criado conforme a interface do brief, com um ajuste necessário descoberto ao testar: o
volume nomeado `iptv-gradle-cache` é criado pelo Docker com dono `root`, e o usuário padrão
da imagem (`circleci`) não tem permissão de escrita nele (erro real:
`Could not create parent directory for lock file ...gradle-9.9.0-bin.zip.lck`). Resolvido
rodando o container com `--user root` — evita um passo de setup manual (`chown`) em
qualquer máquina nova, e é a causa raiz (não um contorno pontual).

### Passo 3 — Wrapper do Gradle

Gerado com `gradle wrapper --gradle-version 9.9.0 --distribution-type bin` usando o
`gradle` da própria imagem (não precisei do `gradle:jdk17`, a imagem já tem `gradle`).

Versão escolhida: **9.9.0**, a atual estável em `services.gradle.org/versions/current`
(consultado em 2026-11-10). Compatibilidade verificada em
`developer.android.com/build/releases/gradle-plugin`: AGP 9.5 exige Gradle mínimo **9.6.0**
— 9.9.0 atende, e o build real (Passo 7) confirmou que funciona de ponta a ponta.

### Passo 4 — Teste que falha

`app/src/test/java/io/github/pagmovel/iptvplayer/EsqueletoTest.kt`, exatamente como no
brief.

### Passo 5 — Ver falhar

`./scripts/gradle.ps1 testDebugUnitTest` com `settings.gradle.kts`/`build.gradle.kts`/
catálogo/wrapper já criados, mas sem `app/build.gradle.kts`:

```
FAILURE: Build failed with an exception.
* What went wrong:
Task not found
  Task 'testDebugUnitTest' not found in root project 'iptv-player-free' and its subprojects.
```

Falha pela razão esperada (sem `app/build.gradle.kts` não existe plugin Android/Kotlin
aplicado, logo a task não existe — equivalente a "sem `BuildConfig`").

### Passo 6 — Implementar o esqueleto

Duas descobertas reais ao implementar (AGP 9.5.1, bem mais novo que o conhecimento
memorizado de AGP), ambas verificadas rodando o build de verdade, não supostas:

1. **A partir do AGP 9.0, o Kotlin já vem embutido** ("built-in Kotlin"). Aplicar
   `org.jetbrains.kotlin.android` quebra o build com um erro explícito apontando para
   `https://kotl.in/gradle/agp-built-in-kotlin`. Removi esse plugin do catálogo e dos dois
   `build.gradle.kts` — só `kotlin.plugin.compose` e `kotlin.plugin.serialization` são
   aplicados.
2. O bloco `kotlinOptions { jvmTarget = "17" }` (do plugin antigo) não existe mais —
   `Unresolved reference 'kotlinOptions'`. O `jvmTarget` do Kotlin embutido segue o
   `compileOptions` (Java) do módulo; removido o bloco.

Implementado:
- `compileSdk` com a sintaxe nova do AGP (confirmada em
  `developer.android.com/reference/tools/gradle-api/9.5/.../CompileSdkSpec`):
  `compileSdk { version = release(37) { minorApiLevel = 2 } }` → API 37.2 (a mais recente
  estável da imagem). `targetSdk = 37` (a doc do manifest Android confirma que não é
  possível expressar versão minor em min/targetSdk, só em compileSdk).
- `minSdk = 26`, `usesCleartextTraffic="true"`, permissão `INTERNET`.
- `buildFeatures { compose = true; buildConfig = true }`.
- `MainActivity` em Compose, Material 3, `darkColorScheme()`, mostrando só
  "IPTV Player Free".
- Ícone adaptativo simples (triângulo de "play" branco sobre fundo escuro `#121212`) — só
  `mipmap-anydpi-v26` (suficiente porque `minSdk=26` já suporta ícone adaptativo nativo,
  sem precisar de fallback legado).
- Catálogo (`gradle/libs.versions.toml`) com todos os aliases pedidos pelo plano —
  `androidx-compose-bom`, `androidx-room-runtime/ktx/compiler`, `androidx-work-runtime`,
  `okhttp`, `okhttp-mockwebserver`, `kotlinx-serialization-json`,
  `kotlinx-coroutines-test`, `robolectric`, `kxml2`, `junit` — todos na última versão
  estável publicada (consultada via `maven-metadata.xml` de `dl.google.com`/
  `repo1.maven.org` em 2026-11-10, filtrando alpha/beta/rc). Só as libs realmente usadas
  pelo esqueleto atual (Compose, core-ktx, activity, lifecycle, junit) entraram em
  `app/build.gradle.kts`; as demais ficam só no catálogo para as próximas tarefas.

### Passo 7 — Ver passar

`./scripts/gradle.ps1 testDebugUnitTest assembleDebug`:

```
BUILD SUCCESSFUL in 5m 15s
45 actionable tasks: 45 executed
```

- Teste: `TEST-io.github.pagmovel.iptvplayer.EsqueletoTest.xml` → `tests="1" failures="0" errors="0"`.
- `app/build/outputs/apk/debug/app-debug.apk` criado (11,6 MB).

### Passo 8 — Commit

Commit `8a47817` (`build: esqueleto do app Android e build via Docker`), autoria corrigida
via variáveis de ambiente (repositório sem `user.name`/`user.email` local — confirmado
antes do commit).

## Autorrevisão

- Testes: suíte unitária rodou de fato (não presumida); vermelho→verde confirmado nos dois
  sentidos com saída real do Gradle.
- Nenhuma suposição não verificada ficou no código: JDK, platforms, usuário/home,
  compileSdk, versões de libs e as duas mudanças de comportamento do AGP 9 vieram de
  execução real ou de documentação oficial lida nesta sessão (não de memória).
- `.gitignore` recebeu as entradas padrão de projeto Android (`build/`, `.gradle/`,
  `local.properties`, `*.apk`, `*.aab`, `.cxx/`, `captures/`, `.idea/`) — não existiam
  antes porque o repositório não tinha código ainda.
- `gradlew` ficou com o bit de execução (`100755`) forçado no índice do git via
  `git update-index --chmod=+x` — sem isso, o Windows grava `100644` e o script perderia a
  permissão de execução ao ser clonado em Linux/CI.
- Não dei `git push` nem criei tags.
- Arquivos de diagnóstico do Passo 1/3 foram para `temp/` (já no `.gitignore`) e apagados
  depois de extrair a informação; nenhum ficou fora do projeto (scratchpad do sistema não
  foi usado para nada que precisasse persistir).

## Pendências para o controlador (fora do escopo deste commit)

1. **Pegadinha corrigida:** `.superpowers/sdd/.gitignore` continha `*`, ignorando toda a
   pasta — contrário à regra do dono de versionar `.superpowers/` por completo. Removi o
   arquivo (`git check-ignore -v .superpowers/sdd/.../progress.md` agora dá saída vazia,
   como a regra pede). Isso deixou `.superpowers/sdd/` inteiro (briefs das 11 tarefas,
   `progress.md`, `plan-path`) como **untracked** — não incluí no meu commit de T1 porque
   não é código da tarefa e pode haver outras tarefas em andamento escrevendo nele; fica
   para o controlador decidir o commit.
2. **Arquivo alheio modificado:** `docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md`
   já estava com alteração não commitada no início desta sessão (atualização do aviso de
   "não executar" para "autorizado... execução em andamento") — não fui eu quem editou
   nesta tarefa. Não incluí no meu commit; segundo a regra do dono, é preciso perguntar
   antes de comitar alteração que não é minha.

## Decisões sem instrução explícita no brief (documentadas, não presumidas às ciegas)

- `rootProject.name = "iptv-player-free"` — nome não estava fixado em nenhum doc do
  projeto; usei o slug do repositório.
- Tema do `<application>` no manifest: `@android:style/Theme.Material.NoActionBar`
  (built-in da plataforma, escuro, zero dependência extra) — a UI em si é 100% Compose/
  Material 3 (`darkColorScheme()`), o tema do manifest só evita um flash claro antes do
  Compose desenhar.
