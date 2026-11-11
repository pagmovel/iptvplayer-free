# IPTV Player Free

Player de IPTV para celular Android (Kotlin), com login Xtream. O APK é gerado pelo GitHub Actions.

- Design aprovado: [docs/superpowers/specs/2026-11-10-iptv-player-free-design.md](docs/superpowers/specs/2026-11-10-iptv-player-free-design.md)
- Esboços das telas: `.superpowers/brainstorm/`
- Plano atual: [docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md](docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md)

## Onde paramos (2026-11-11, pausado a pedido do dono — fim do dia, T3 quase completa)

- Execução do Plano 1 via **superpowers:subagent-driven-development**. **Concluídas,
  testadas e revisadas: T1, T4, T5, T6, T7, T8, T9, T10, T11** (passos 1–6 e 9; 9 de 11
  tarefas). **T2 concluída, commitada e testada de verdade no Actions** (push da T3 fez o
  job `testes` rodar e passar: run `43083373024`). **T3 com código/chave prontos, mas a
  primeira Release pública (`v0.0.2`) ainda não terminou de rodar** — ver abaixo.
- **T2** (`0e5021d`, `.github/workflows/build.yml` + `app/build.gradle.kts` + `.gitignore`):
  `versionCode`/`versionName` de `GITHUB_RUN_NUMBER`/`VERSAO_APP`; `signingConfigs.release`
  condicional a `app/assinatura.jks` existir; job `testes` (push/PR) e job `release` (só tag
  `v*`, decodifica o keystore do Secret, monta e assina dentro do mesmo container
  `cimg/android:2026.10`, publica a Release com `gh release create` rodando fora do
  container — a imagem não tem `gh`). Confirmado rodando de verdade no Actions.
- **T3 — achado real e corrigido nesta sessão:** a primeira chave de assinatura foi gerada
  com o `keytool` do **JDK 8 local** (`jre1.8.0_441`), como o passo 1 do plano mandava. A tag
  `v0.0.1` rodou o job `release` de verdade e **falhou**: `KeytoolException: ... Password is
  not ASCII`, mesmo a senha sendo hexadecimal (ASCII puro). Causa raiz confirmada com debug
  sistemático (comparei os JDKs: local `1.8.0_441` vs. **OpenJDK 21** dentro do
  `cimg/android:2026.10`, que é quem assina no CI de verdade; testei gerar+assinar com o
  mesmo JDK 21 → `BUILD SUCCESSFUL` + `apksigner verify` confirmou o certificado). **Chave
  real de produção foi regenerada dentro do `cimg/android:2026.10`** (mesmo JDK que assina),
  verificada de ponta a ponta localmente antes de seguir. Processo completo e as pegadinhas
  (incluindo duas que eram ruído do bind mount do Windows, não da causa real) estão em
  `.claude/skills/iptv-build-assinatura/SKILL.md` — **leia antes de mexer em chave/assinatura
  de novo**.
- Como a `v0.0.1` falhou sem publicar Release nenhuma (confirmado: 404 em
  `/releases/tags/v0.0.1`), o dono decidiu pular direto para **`v0.0.2`** em vez de recriar a
  `v0.0.1`. Tag `v0.0.2` criada e enviada (run `43097855518`).
- **Esse run ficou travado em `queued` por mais de 30 minutos** — não é problema nosso:
  confirmado por `https://www.githubstatus.com` que há uma **interrupção parcial real da
  plataforma GitHub agora**, afetando Actions/Pages/Copilot (status passou de
  "investigating" para "monitoring" antes da pausa, ainda não "resolved"). Nada a fazer do
  nosso lado além de esperar.
- **[A VERIFICAR] se os 4 Secrets no GitHub já estão com os valores NOVOS** (os da chave
  regenerada, em `temp/assinatura/valores-secrets.txt`) — o dono disse ter cadastrado os
  Secrets antes da troca de chave; depois da troca, pedi para **atualizar** (não criar de
  novo) com os valores novos, mas a confirmação ficou ambígua na conversa (o dono reclamou de
  pergunta repetida). **Não supor**: se o run da `v0.0.2` falhar de novo com a mesma
  `KeytoolException`/"Password is not ASCII", é sinal de que os Secrets ainda estão com a
  chave antiga — nesse caso, pedir para atualizar os 4 com os valores de
  `temp/assinatura/valores-secrets.txt` (gerados dentro do container, chave boa) antes de
  tentar de novo.
- **Próxima sessão:** checar o status do run `43097855518` (tag `v0.0.2`) —
  `https://github.com/pagmovel/iptvplayer-free/actions/runs/43097855518` — e o incidente em
  `https://www.githubstatus.com`. Se passou: confirmar a Release `v0.0.2` publicada com
  `iptv-player-free-0.0.2.apk` (passo 5 do plano T3), e seguir pro passo 6 (dono instala no
  celular, valida). Se falhou por `Password is not ASCII` de novo: é os Secrets desatualizados,
  não um bug novo — atualizar e tentar outra tag (`v0.0.3`?) com um novo "sim". Se falhou por
  outro motivo: log real da página do Actions antes de supor a causa (API de logs dá 403 sem
  `gh`/token nesta máquina — já documentado na skill).
- Pendência aberta, resolvida nesta sessão: `fwcd.kotlin-0.2.36.vsix` era uma extensão do
  VSCode baixada a pedido meu numa sessão anterior; o dono decidiu **deixar como está** na
  raiz, sem versionar — não perguntar de novo.
- Working tree limpo, tudo commitado e enviado (`f3cc691` é o HEAD de `main`).
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
