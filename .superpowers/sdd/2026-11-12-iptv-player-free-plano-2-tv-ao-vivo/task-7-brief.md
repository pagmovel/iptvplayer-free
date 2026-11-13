### T7: Tela Ao Vivo — aba Canais

**Arquivos:**
- Criar: `ui/aovivo/AoVivoViewModel.kt`, `ui/aovivo/AoVivoTela.kt`, `ui/aovivo/CanaisAba.kt`
- Modificar: `ui/Navegacao.kt`, `ui/resumo/ResumoTela.kt` (botão "Assistir ao vivo")
- Teste: nenhum automatizado de Compose nesta fase (mesmo padrão do Plano 1 — UI valida no
  celular, T11); `AoVivoViewModel` tem a parte não-Compose testável se tiver lógica própria
  além de repassar chamadas (decisão de quem implementar: só escrever teste se sobrar lógica
  não-trivial fora do que T1/T3/T5 já cobrem).

**Interfaces:**
- Consome: `CatalogoDao.canais/categorias/favoritos` (T3), `agruparPorQualidade` (T1),
  `CanalFavorito` (T2), `ContasRepositorio.credenciais`/`contaAtiva` (Plano 1),
  `CascataQualidade`/`EventoCascata` (T5), `PlayerAoVivo` (T6),
  `ConfiguracoesRepositorio.tempoBufferMs()` (T4, lido a cada `selecionarCanal` — uma troca
  feita em Configurações só vale a partir do próximo canal aberto, não precisa recriar a
  cascata do canal já tocando).
- Produz: `AoVivoViewModel` expõe os canais agrupados já filtrados por categoria/favoritos
  selecionados, o estado atual do player (`Tocando`/`Trocou`/`FalhaTerminal`, via
  `cascata.eventos`), e as ações `selecionarCanal(CanalAgrupado)` (cria uma `CascataQualidade`
  nova pro canal e chama `playerAoVivo.abrir(canal, cascata)`), `favoritar`/`desfavoritar`,
  `filtrarPorCategoria(categoriaId: Long?)` (`null` = Todos), `filtrarFavoritos(Boolean)`,
  `tentarNovamente()` (repassa pra `cascata.tentarNovamente()`), `pausar()`/`retomar()`
  (chamados pelo ciclo de vida da tela, passo 4 abaixo).

- [ ] **Passo 1: Implementar `AoVivoViewModel`** combinando `canais()` + `categorias()` +
  `favoritos()` (T3) com `agruparPorQualidade` (T1) e o filtro ativo, igual ao padrão de
  `ResumoViewModel`/`ContasViewModel` (classe comum, sem `androidx.lifecycle.ViewModel`).
  `selecionarCanal` guarda a `CascataQualidade` ativa (uma por vez — trocar de canal cancela
  a anterior) e liga `playerAoVivo.abrir(canal, cascata)`.
- [ ] **Passo 2: Implementar `CanaisAba`** — chips de categoria (incluindo "Favoritos" e
  "Todos", design §4), lista de `CanalAgrupado` com nome, ícone (ou nome sobre fundo neutro,
  mesma regra do design §4 "Navegação"), estrela de favorito, e o programa atual do canal
  **só se** a janela de hoje já tiver sido carregada (reaproveita `programacao`, T3, com a
  janela do instante atual) — "Sem programação" se vazio, nunca espaço em branco.
- [ ] **Passo 3: Implementar `AoVivoTela`** — hospeda o `PlayerAoVivo` (T6) via `AndroidView`
  wrapping `PlayerView`, a `CanaisAba` abaixo (parte da tela dividida), e um `TabRow` simples
  pra alternar com a aba Programação (T8). Quando o estado do player for `FalhaTerminal`
  (Global Constraints — qualidade mais baixa também falhou), mostra uma mensagem de erro por
  cima do vídeo com um botão "Tentar de novo" chamando `viewModel.tentarNovamente()` — nunca
  tenta sozinho sem esse toque.
- [ ] **Passo 4: Parar o vídeo em segundo plano** (Global Constraints). `AoVivoTela` observa o
  ciclo de vida (`LifecycleEventObserver`/`DisposableEffect` com `LocalLifecycleOwner`):
  `ON_STOP` chama `viewModel.pausar()` (`exoPlayer.pause()` dentro de `PlayerAoVivo`, sem
  liberar o player); `ON_START` chama `viewModel.retomar()` (volta a tocar o canal atual do
  zero — reabrir, não é retomar posição, já que é transmissão ao vivo). `ON_DISPOSE` libera o
  player de verdade (`PlayerAoVivo.liberar()`, T6) pra não vazar o `ExoPlayer` ao sair da tela.
- [ ] **Passo 5: Ligar a navegação.** `Navegacao.kt` ganha `Tela.AO_VIVO`; `ResumoTela` ganha
  o botão "Assistir ao vivo" ao lado de "Gerenciar contas" *(decisão minha: sem o bottom bar
  completo de 4 abas do design ainda — Filmes/Séries não existem até o Plano 3; construir a
  barra inteira agora deixaria 2 abas mortas. A barra de verdade entra no plano que criar a
  última das 4 telas)*.
- [ ] **Passo 6: Rodar `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`.** Esperado:
  `BUILD SUCCESSFUL`.
- [ ] **Passo 7: Commit.** `feat: tela de TV ao vivo -- aba Canais`

