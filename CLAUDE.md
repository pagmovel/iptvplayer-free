# IPTV Player Free

Player de IPTV para celular Android (Kotlin), com login Xtream. O APK é gerado pelo GitHub Actions.

- Design aprovado: [docs/superpowers/specs/2026-11-10-iptv-player-free-design.md](docs/superpowers/specs/2026-11-10-iptv-player-free-design.md)
- Esboços das telas: `.superpowers/brainstorm/`
- Plano atual: [docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md](docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md)

## Onde paramos (2026-11-12, primeira Release pública publicada; falta a validação no celular)

- Execução do Plano 1 via **superpowers:subagent-driven-development**. **Concluídas,
  testadas e revisadas: T1, T4, T5, T6, T7, T8, T9, T10, T11** (passos 1–6 e 9; 9 de 11
  tarefas). **T2 e T3 testadas de verdade no Actions, com Release pública publicada.** Falta
  só o passo 6 da T3 (dono instalar a `v0.0.3` no celular e validar a atualização por cima) e
  os passos 7–8 da T11 (que dependem da mesma validação) para a Fase 0 virar `validada`.
- **T2** (`0e5021d`): workflow `.github/workflows/build.yml` + `app/build.gradle.kts` +
  `.gitignore`. Job `testes` (push/PR) e job `release` (só tag `v*`) rodando de verdade no
  Actions.
- **T3 — achado real e corrigido em 2026-11-11:** a primeira chave de assinatura foi gerada
  com o `keytool` do **JDK 8 local**, incompatível com o **OpenJDK 21** de dentro do
  `cimg/android:2026.10` (quem assina no CI de verdade) — tag `v0.0.1` falhou com
  `KeytoolException: ... Password is not ASCII`. Causa raiz confirmada com debug sistemático
  e corrigida gerando a chave **dentro do próprio container**. Processo e pegadinhas em
  `.claude/skills/iptv-build-assinatura/SKILL.md` — **leia antes de mexer em chave/assinatura
  de novo**.
- **Histórico das tentativas de tag** (detalhe completo no ledger do SDD):
  `v0.0.1` falhou (chave incompatível, sem Release). `v0.0.2` ficou presa em `queued` por um
  **outage real da plataforma GitHub** (Actions/Pages/Copilot, confirmado em
  `githubstatus.com`) — mesmo depois do outage resolvido, o run não saiu do estado nem o
  "Cancel workflow" da UI funcionou; ficou órfã, sem Release, e foi abandonada.
  **`v0.0.3` (run `43232261797`, 2026-11-12) passou de verdade**: `testes` = `success`,
  `release` = `success`.
- **Primeira Release pública do projeto:**
  https://github.com/pagmovel/iptvplayer-free/releases/tag/v0.0.3 — anexo
  `iptv-player-free-0.0.3.apk` (9.390.406 bytes, mesmo tamanho verificado localmente com
  `apksigner verify` antes do cadastro dos Secrets).
- Secrets cadastrados pelo dono com a chave antiga, depois **atualizados por ele** com os
  valores novos em 2026-11-12 (confirmado pelo "Last updated" de cada um dos 4 na página de
  Secrets, não por suposição).
- **Próxima sessão / passo 6 da T3:** pedir pro dono instalar a `v0.0.3` no celular. Depois,
  mudar o texto da tela (versão seguinte), publicar outra tag (com "sim" específico) e pedir
  pra instalar **por cima**, sem desinstalar — esperado: sem "conflito com pacote existente".
  Só então a Fase 0 vira `validada` de verdade (passos 7–8 da T11 dependem disso também).
- `fwcd.kotlin-0.2.36.vsix` na raiz: o dono decidiu deixar como está, sem versionar — não
  perguntar de novo.
- Working tree limpo, tudo commitado e enviado.
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
