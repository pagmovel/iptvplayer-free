### T10: Tela cheia ao deitar + navegação + Release v0.2.0

**Arquivos:**
- Modificar: `ui/aovivo/AoVivoTela.kt`
- Criar: `ui/configuracoes/ConfiguracoesViewModel.kt`, `ui/configuracoes/ConfiguracoesTela.kt`
- Modificar: `ui/Navegacao.kt`, `.claude/skills/iptv-xtream-carga/SKILL.md` ou nova skill
  `iptv-tv-ao-vivo` (documentar a cascata de qualidade e os achados desta fase)

**Interfaces:**
- Consome: `ConfiguracoesRepositorio` (T4).
- Produz: `Tela.CONFIGURACOES` em `Navegacao.kt`; `ConfiguracoesTela` com o campo do tempo de
  buffer (segundos, padrão 8).

- [ ] **Passo 1: Detectar orientação em `AoVivoTela`** via
  `LocalConfiguration.current.orientation` — `ORIENTATION_LANDSCAPE` mostra só o vídeo em
  tela cheia (sem as abas Canais/Programação, que ficam atrás do OSD, T9, como overlay);
  `ORIENTATION_PORTRAIT` volta pra tela dividida.
- [ ] **Passo 2: Implementar `ConfiguracoesTela`/`ConfiguracoesViewModel`** — único campo:
  tempo de buffer em segundos, lendo/gravando em `ConfiguracoesRepositorio` (T4). Acessível
  pelo atalho do OSD (T9) e de onde "Gerenciar contas" já é hoje (`ResumoTela`, Plano 1).
- [ ] **Passo 3: Rodar `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`.** Esperado:
  `BUILD SUCCESSFUL`.
- [ ] **Passo 4: Commit.** `feat: tela cheia ao deitar, tela de Configuracoes e navegacao`
- [ ] **Passo 5: Documentar.** Nova skill `.claude/skills/iptv-tv-ao-vivo/SKILL.md` com: o
  algoritmo de agrupamento por sufixo (e a ressalva de que só `SD`/`HD`/`FHD`/`4K` foram
  confirmados contra o catálogo real,§5 do design), a máquina de estados da cascata, e
  qualquer achado real do passo de validação (T11). Atualizar a tabela de acompanhamento
  deste plano e a §6 do design.
- [ ] **Passo 6: Com o "sim" do dono** (ou a autorização permanente já concedida para tags de
  versão), publicar a tag `v0.2.0`.

