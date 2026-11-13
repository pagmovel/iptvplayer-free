# IPTV Player Free

Player de IPTV para celular Android (Kotlin), com login Xtream. O APK é gerado pelo GitHub Actions.

- Design aprovado: [docs/superpowers/specs/2026-11-10-iptv-player-free-design.md](docs/superpowers/specs/2026-11-10-iptv-player-free-design.md)
- Esboços das telas: `.superpowers/brainstorm/`
- Plano 1 (concluído): [docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md](docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md)
- Plano atual: [docs/superpowers/plans/2026-11-12-iptv-player-free-plano-2-tv-ao-vivo.md](docs/superpowers/plans/2026-11-12-iptv-player-free-plano-2-tv-ao-vivo.md)

## Onde paramos (2026-11-13, Plano 2 · TV ao vivo T1-T10 completo e taggeado; T11 em validação real; brainstorm do OSD em andamento)

- **Plano 1** está `validada` de ponta a ponta em produção (última Release `v0.1.0`). Ledger:
  `.superpowers/sdd/2026-11-10-iptv-player-free-plano-1-base/progress.md` — não repetir aqui.
- **Plano 2 (TV ao vivo), T1-T10 completo via subagent-driven-development**: cada tarefa
  implementada + revisada + corrigida; revisão final de toda a branch achou 3 Crítico + 6
  Importante reais (ex. timer da cascata travava, tela apagava, vídeo tocava em segundo
  plano) — corrigidos e re-revisados antes da tag. **Release `v0.2.0` publicada.** Detalhe
  completo (achados, rulings, diffs): ledger
  `.superpowers/sdd/2026-11-12-iptv-player-free-plano-2-tv-ao-vivo/progress.md`.
- **T11 (validação real do dono, no celular, contra o provedor) em andamento.** Já rendeu 2
  patches publicados a partir de bugs reais encontrados na validação (não achados de revisão
  de código): **`v0.2.1`** (modo imersivo real — barras do sistema escondidas em paisagem,
  nunca implementado antes) e **`v0.2.2`** (controle nativo do ExoPlayer competindo com o
  nosso OSD — `useController=false`; spinner de buffering, que não existia). Ambos
  implementados, revisados e taggeados — ver ledger acima pra detalhe técnico.
- **Validando o OSD, o dono rejeitou o layout atual** (lista de botões de texto empilhados) e
  pediu mockups. **Brainstorm em andamento** (skill `superpowers:brainstorming`, visual
  companion rodando em `.superpowers/brainstorm/921-1794583402/`). Decisões já fechadas
  (não perguntar de novo):
  - Layout: Opção A (controles em overlay sobre o vídeo, ícones universais — não a Opção B
    de barras separadas tipo "Sample channel 1"), com scrim escuro atrás dos controles.
  - Qualidade (SD/HD/FHD): visível só quando o OSD está aberto, ou ao trocar — não atrás de
    engrenagem separada.
  - Atalhos do app (Programação/Gerenciar contas/Configurações): ícones próprios na barra de
    baixo do OSD, não um menu "mais opções".
  - Logo do canal: no OSD e no banner de troca (estilo TV a cabo — aparece/some sozinho,
    mesmo padrão do selo "Mudou para X" já existente) — **não** na lista de canais por
    enquanto (precisaria de lib de imagem nova, Coil; escopo contido só ao OSD/banner).
  - Play/pause: volta pro centro do OSD (⏮ / ⏸▶ / ⏭) — lacuna real que tinha sumido ao
    desativar o controle nativo do ExoPlayer (v0.2.2), já incluída nos mockups desde a v1.
  - Spinner de carregamento do catálogo (canais/filmes/séries): anel dourado com
    **porcentagem real** (não só visual) — vai precisar de lógica nova de progresso, maior
    que só UI.
  - **Playback completo enviado ao dono em 2026-11-13** (recapitulando todas as decisões
    acima, com o play/pause explicitado) — **ainda sem confirmação dele**. Próxima sessão
    começa lendo a resposta dele a esse playback antes de qualquer outra coisa.
  - **Adiado explicitamente pelo dono pra outra sessão** (não misturar com o OSD agora):
    cor de destaque geral do app (rejeitou o azul `#4FC3F7` na prática, mesmo tendo aprovado
    antes — ver memória `.claude/memory/plano2-feedback-design-t11.md`); fluxo de abertura
    do app (tela de carregamento dedicada entre conta ativada e app pronto, hoje o progresso
    é inline na tela de Resumo); redesign da tela de início (já sabida como provisória).
- **Próxima sessão**: ler a resposta do dono ao playback do OSD (confirmação ou ajuste) →
  builder-check → design doc → `superpowers:writing-plans` → implementar via
  subagent-driven-development → nova tag de patch → retomar T11. **Nenhuma linha de código do
  OSD foi escrita ainda** — é só brainstorm/mockup até aqui (gate da skill de brainstorming:
  nada se constrói sem aprovação da descrição completa).
- **`v0.2.2` continua sendo a última release publicada** — nada mudou em `app/` desde então
  (só docs/memória/mockups), por isso nenhuma tag nova foi gerada nesta sessão. Só cortar tag
  nova quando houver mudança real de código.
- Fora do git, de propósito: `listas.txt`, `.env` (credenciais — ver skill
  `iptv-xtream-carga` item 10), `temp/`.
- `fwcd.kotlin-0.2.36.vsix` na raiz: o dono decidiu deixar sem versionar.
- Docker Desktop local precisa estar rodando para `./scripts/gradle.ps1`.

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
