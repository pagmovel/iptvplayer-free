### T9: OSD do player

**Arquivos:**
- Criar: `ui/aovivo/Osd.kt`
- Modificar: `ui/aovivo/AoVivoTela.kt`, `ui/aovivo/AoVivoViewModel.kt`

**Interfaces:**
- Consome: `CascataQualidade.eventos` (T5, pro selo de troca de qualidade), navegação para
  Configurações/Gerenciar contas (T4, Plano 1).
- Produz: `Osd` composable recebendo `visivel: Boolean`, `aoTocarNaTela: () -> Unit` (reseta o
  timer de auto-esconder), e as ações (voltar, trocar qualidade manualmente, canal
  anterior/próximo, abrir grade de programação como overlay, Gerenciar contas, Configurações).

- [ ] **Passo 1: Implementar o estado de visibilidade** em `AoVivoTela` — `mutableStateOf(true)`
  que vira `false` depois de alguns segundos sem toque (`LaunchedEffect` com `delay`,
  reiniciado a cada toque), sobre um `Box` que detecta toque no vídeo.
- [ ] **Passo 2: Implementar `Osd`** — barra com voltar, nome do canal + programa atual
  (reaproveita a mesma busca de "programa atual" da `CanaisAba`, T7), seletor de qualidade
  (lista só as `variantes` do `CanalAgrupado` atual, T1), canal anterior/próximo (navega
  dentro da lista filtrada ativa no momento, T7 — *decisão minha*, ver design), atalho pra
  grade de programação (abre `ProgramacaoAba`, T8, como overlay por cima do vídeo em tela
  cheia), e atalhos pra Gerenciar contas / Configurações. Selo rápido de "Mudou para X" ao
  receber um evento `Trocou` de `CascataQualidade` (também um `LaunchedEffect` com `delay`
  pra sumir sozinho).
- [ ] **Passo 3: Rodar `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`.**
- [ ] **Passo 4: Commit.** `feat: OSD do player de TV ao vivo`

