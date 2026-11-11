# IPTV Player Free

Player de IPTV para celular Android (Kotlin), com login Xtream. O APK é gerado pelo GitHub Actions.

- Design aprovado: [docs/superpowers/specs/2026-11-10-iptv-player-free-design.md](docs/superpowers/specs/2026-11-10-iptv-player-free-design.md)
- Esboços das telas: `.superpowers/brainstorm/`
- Plano atual: [docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md](docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md)

## Onde paramos (2026-11-11, pausado a pedido do dono em pleno T2)

- Execução do Plano 1 via **superpowers:subagent-driven-development**. **Concluídas,
  testadas e revisadas: T1, T4, T5, T6, T7, T8, T9, T10, T11** (passos 1–6 e 9; 9 de 11
  tarefas). T2 e T3 continuavam `pendente` até eu começar a T2 nesta sessão (ver abaixo).
- Perguntei ao dono sobre o gap de T2/T3; ele confirmou (2026-11-11): "revise o plano,
  porque vc tinha dito que vc mesmo geraria a chave" — ou seja, seguir o plano como escrito:
  eu (controlador, nunca subagente) conduzo T2 e depois T3, com o "sim" dele a cada push/tag.
- **T2 em andamento, código escrito mas NÃO COMITADO (verificação não terminou):**
  - `app/build.gradle.kts`: `versionCode`/`versionName` agora lêem `GITHUB_RUN_NUMBER`/
    `VERSAO_APP` (com os defaults `1`/`"0.0.0-dev"`); `signingConfigs.release` só existe se
    `app/assinatura.jks` existir (nunca localmente — decodificado do Secret só no CI).
  - `.gitignore`: adicionado `*.jks`.
  - `.github/workflows/build.yml` (novo): job `testes` (push/PR, roda `testDebugUnitTest
    assembleDebug` dentro do mesmo container `cimg/android:2026.10` do dev local, via `docker
    run` — **não** usa o Android SDK que viesse no runner `ubuntu-latest`, para garantir a
    mesma versão já validada nesta sessão); job `release` (só em tag `v*`, depende de
    `testes`, decodifica o keystore do Secret, monta o `assembleRelease` assinado no mesmo
    container, renomeia para `iptv-player-free-<versão>.apk`, publica a Release com `gh
    release create` — isso roda fora do container, direto no runner, porque **confirmei com
    `docker run cimg/android:2026.10 bash -lc "which gh"` que a imagem não tem `gh`
    instalado**, então só a montagem do APK roda dentro do container; o `gh` do runner faz a
    Release).
  - **Rodei a verificação local do passo 3 do brief** (`./scripts/gradle.ps1
    testDebugUnitTest assembleDebug assembleRelease`) e ela **não terminou** (timeout de
    10 min no meio de `optimizeReleaseResources`, provavelmente só a primeira vez compilando
    a variante `release`, nunca compilada antes neste projeto — pode ser normal, não
    necessariamente um problema). **Parei o container a pedido do dono antes de saber o
    resultado.** Por isso **não commitei nada do T2** — regra do projeto é não commitar sem
    ver passar de verdade.
- **Próxima sessão:** rodar de novo `./scripts/gradle.ps1 testDebugUnitTest assembleDebug
  assembleRelease` (pode dar mais tempo que o timeout de 10 min da ferramenta — considerar
  rodar em background ou com timeout maior) e só commitar o T2 depois de ver `BUILD
  SUCCESSFUL` com o release **sem assinatura** (sem `app/assinatura.jks` presente). Depois
  disso: T3 (gerar a chave real com `keytool`, salvar em `temp/assinatura/` — fora do git —,
  e guiar o dono no cadastro dos 4 Secrets no GitHub, já que esta máquina não tem `gh` CLI
  instalado, o cadastro dos Secrets tem que ser pela interface web). Push e tag continuam
  precisando do "sim" específico na hora, nunca inferido de uma autorização anterior.
- Estado do git nesta pausa: `.gitignore` e `app/build.gradle.kts` modificados,
  `.github/workflows/build.yml` novo — **tudo não comitado, de propósito**. `.kotlin/` (cache
  do Gradle/Kotlin, gerado pela tentativa de build) ficou untracked, não é pra comitar.
- O bloqueio de HTTP 429 do Maven Central no artefato do Robolectric
  (`org.robolectric:nativeruntime-dist-compat:1.0.19`) foi contornado em `settings.gradle.kts`
  com `exclusiveContent { forRepository { maven(espelho do Google) }; filter {
  includeModule(...) } }` — força esse módulo específico a vir só do espelho, sem passar pelo
  Maven Central. Declarar o espelho como repositório extra solto (tentativa anterior) **não
  funcionava**: o Gradle resolve o POM pelo `mavenCentral()` e não tenta o próximo repositório
  para o binário do mesmo módulo. T10/T11 também usam Robolectric de verdade e já se beneficiam
  dessa correção. Docker Desktop local precisa estar rodando para o `./scripts/gradle.ps1`
  funcionar — `docker desktop start` se estiver parado.
- T9 (`carga/CargaCatalogo.kt`/`CargaCatalogoTest.kt`) e T10 (`conta/Cofre.kt`,
  `conta/ContaDao.kt`, `conta/ContasRepositorio.kt`) commitadas, verificadas de fato e
  revisadas — T10 teve 1 achado Importante real (`ativar()` sem transação, corrigido).
  Suíte completa do projeto passa de ponta a ponta: **8 classes, 41 testes, 0 falhas**.
  Detalhe completo no ledger do SDD
  (`.superpowers/sdd/2026-11-10-iptv-player-free-plano-1-base/progress.md`).
- Só há commits locais; **nada foi enviado ao GitHub**. Push e tag dependem do "sim" do dono na
  hora (T3 e T11 do plano).
- Fora do git: `listas.txt` (credenciais) e `temp/` (`coletar_amostras.py` e
  `temp/amostras/`, base de formato para os testes; nunca copiar os dados para testes
  versionados).

## Regras do projeto

- `listas.txt` (credenciais do dono) e `temp/` (amostras do provedor) **nunca** vão para o
  repositório, que é público.
- Testes versionados usam só dados sintéticos.
- A chave de assinatura fica fora do repositório e nos GitHub Secrets (ver design, §4).
- **Autoria dos commits (decisão do dono em 2026-11-10, só para este projeto):** a partir do
  commit `90e61b7`, autor e committer são `Marcos Ronaldo <marcos.ronaldo@gmail.com>` — não
  `marcossilva@rms.adv.br` (regra global) nem o e-mail do git config global desta máquina.
  Commits anteriores a esse não foram reescritos. Repositório sem `user.name`/`user.email`
  local: sempre setar as 4 variáveis de ambiente (`GIT_AUTHOR_NAME`/`_EMAIL`,
  `GIT_COMMITTER_NAME`/`_EMAIL`) no comando do commit, nunca mexer no `git config`.

## Plugins e skills usados

- **superpowers** (plugin): brainstorming, writing-plans e execução do plano.
- **context-mode** (plugin): leitura e execução com saída filtrada.
- **impeccable** (plugin): hook de qualidade visual. As exceções ficam em `.impeccable/config.json`.
