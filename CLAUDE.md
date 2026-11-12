# IPTV Player Free

Player de IPTV para celular Android (Kotlin), com login Xtream. O APK é gerado pelo GitHub Actions.

- Design aprovado: [docs/superpowers/specs/2026-11-10-iptv-player-free-design.md](docs/superpowers/specs/2026-11-10-iptv-player-free-design.md)
- Esboços das telas: `.superpowers/brainstorm/`
- Plano 1 (concluído): [docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md](docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md)
- Plano atual: [docs/superpowers/plans/2026-11-12-iptv-player-free-plano-2-tv-ao-vivo.md](docs/superpowers/plans/2026-11-12-iptv-player-free-plano-2-tv-ao-vivo.md)

## Onde paramos (2026-11-12, Plano 2 · TV ao vivo planejado e aprovado; execução não iniciada)

- **Plano 1 (build/CI/assinatura + contas/carga de dados) está `validada` de ponta a ponta em
  produção**, com conta real (última Release: `v0.1.0`,
  https://github.com/pagmovel/iptvplayer-free/releases/tag/v0.1.0). Histórico completo de
  releases, achados reais pós-lançamento e decisões de permissão do Claude Code: ledger do SDD
  (`.superpowers/sdd/2026-11-10-iptv-player-free-plano-1-base/progress.md`) — não repetir aqui.
- **Plano 2 (TV ao vivo) passou por brainstorming + builder-check e foi aprovado pelo dono**.
  Design amendado em `docs/superpowers/specs/2026-11-10-iptv-player-free-design.md` (seção
  "TV ao vivo"): canais agrupados por qualidade SD/HD/FHD/4K (confirmado com dados reais do
  provedor — sufixo no nome, ex. "WARNER CHANNEL FHD/HD/SD"), troca automática em cascata
  (timer de 8s configurável, reinicia por tentativa, erro definitivo troca na hora, para com
  aviso se até o SD falhar) e manual, OSD do player, e tela nova de Configurações.
- **Plano escrito** em `docs/superpowers/plans/2026-11-12-iptv-player-free-plano-2-tv-ao-vivo.md`
  (11 tarefas, T1–T11), auto-revisado contra a skill `iptv-xtream-carga` (achado real já
  confirmado: `epg_channel_id` é igual entre variantes de qualidade do mesmo canal — item 4).
  **Execução ainda não começou.**
- **Próxima sessão**: escolher a abordagem de execução (subagent-driven ou nativa — perguntar
  ao dono se ainda não escolheu) e começar pela T1 (agrupamento de canais por qualidade).
- Fora do git, de propósito: `listas.txt` (credenciais do dono), `.env` (credenciais de
  teste — tem seções "lista de servidores" e "LISTA M3U N"; a segunda tem credenciais
  embutidas na URL, cuidado ao filtrar candidatos pra testar, ver skill `iptv-xtream-carga`
  item 10), `temp/` (amostras/diagnósticos reais — nunca em teste versionado).
- `fwcd.kotlin-0.2.36.vsix` na raiz: o dono decidiu deixar como está, sem versionar.
- Docker Desktop local precisa estar rodando para `./scripts/gradle.ps1` funcionar.

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
