# T10: Tela cheia ao deitar + navegação + Release v0.2.0 — Passos 1 a 5

Escopo: só os Passos 1-5 do brief (`task-10-brief.md`). Passo 6 (tag `v0.2.0`) é do
controlador — não foi tocado aqui (nenhum comando de tag/release/push).

## O que foi implementado

### Passo 1 — Orientação em `AoVivoTela`

`app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoTela.kt`:

- `val emPaisagem = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE`.
- Em paisagem: `Box` do vídeo vira `Modifier.fillMaxSize()` (antes `.fillMaxWidth().height(220.dp)`);
  o `TextButton("Voltar")` externo, o `TabRow` e o `Box` das abas Canais/Programação ficam
  dentro de `if (!emPaisagem) { ... }` e não renderizam.
- O `Osd` e o overlay de grade (`if (grade) { ... }`) não foram tocados na lógica interna —
  só o que fica visível **em volta** deles mudou, como o brief pediu.
- Nenhum `remember`/chave nova depende da orientação, então girar o celular não força
  recriação de nada dentro da árvore — e o manifest já tem
  `configChanges="orientation|screenSize|screenLayout"` (T6), então a `Activity` em si também
  não recria. `PlayerView`/`ExoPlayer` sobrevivem à rotação.

### Passo 2 — Configurações

- Criado `ui/configuracoes/ConfiguracoesViewModel.kt`: classe simples (sem
  `androidx.lifecycle.ViewModel`, mesmo padrão de `ResumoViewModel`/`AoVivoViewModel`), só
  delega pra `ConfiguracoesRepositorio` (T4) com a conversão ms↔segundos isolada em
  `tempoBufferSegundos()`/`definirTempoBufferSegundos(Int)`.
- Criado `ui/configuracoes/ConfiguracoesTela.kt`: um `OutlinedTextField` numérico (segundos),
  botão "Salvar" (só grava se o texto for um inteiro > 0) e "Voltar".
- `ui/Navegacao.kt`: adicionado `Tela.CONFIGURACOES`; o no-op `onAbrirConfiguracoes = {}` da
  T9 foi substituído por navegação real nos dois pontos de entrada (`ResumoTela` e
  `AoVivoTela`/OSD). Guardei qual tela abriu Configurações num
  `telaAntesDeConfiguracoes` pra "Voltar" retornar pro lugar certo (Resumo ou Ao vivo), em vez
  de sempre cair no Resumo.
- `ui/resumo/ResumoTela.kt`: novo parâmetro `onAbrirConfiguracoes: () -> Unit` e botão
  "Configurações" ao lado de "Gerenciar contas" (mesmo padrão usado pra "Assistir ao vivo" na
  T7). Único caller (`Navegacao.kt`) já atualizado — confirmado por busca, não há outro lugar
  instanciando `ResumoTela`.

### Teste automatizado

`app/src/test/java/io/github/pagmovel/iptvplayer/ui/configuracoes/ConfiguracoesViewModelTest.kt`
(Robolectric, mesmo padrão de `ConfiguracoesRepositorioTest`): 2 testes cobrindo o valor
padrão (8s) e a conversão ms↔segundos ida e volta — única lógica não trivial do ViewModel
(o resto é puro delegate pro repositório já testado).

## Decisões de layout condicional

- `LocalConfiguration.current` dentro do `@Composable`, sem `DisposableEffect`/listener
  próprio — Compose já recompõe sozinho quando a orientação muda.
- Nada de duplicar a lógica do `Osd`/overlay de grade: só a visibilidade do que está em volta
  deles mudou. O `Osd` já tinha seu próprio "Voltar", seletor de qualidade, canal
  anterior/próximo e os dois atalhos (Gerenciar contas/Configurações) desde a T9 — paisagem
  não precisou de nada novo ali.

## Conteúdo da skill nova (resumo)

`.claude/skills/iptv-tv-ao-vivo/SKILL.md` — documenta:

1. **Agrupamento por sufixo (T1)**: regex `(?i)\s+(4K|FHD|HD|SD)$`, chave
   `nomeBase + categoriaId`, com a ressalva `[A VERIFICAR]` de que só esses 4 sufixos foram
   confirmados contra o catálogo real (design §5) — sufixo não reconhecido cai no fallback
   `Qualidade.SD` sem quebrar, mas pode rotular errado.
2. **Máquina de estados da cascata (T5)**: `aoTravar`/`aoEstabilizar`/`aoErroDefinitivo`,
   timer que reinicia por tentativa, `FalhaTerminal` sem retry automático (limite de 2
   conexões do provedor), e o achado real da T9 — `escolherManualmente()` precisa emitir
   `EventoCascata.Trocou` na hora, não só trocar o índice interno, senão o seletor manual do
   OSD fica mudo.
3. **OSD/orientação (T9/T10)** e **Configurações (T4/T10)**: os achados desta tarefa — acerto
   de não precisar de listener próprio pra orientação, dependência do `configChanges` da T6,
   conversão ms↔segundos isolada no ViewModel, e navegação com "voltar pro lugar certo".

Nenhum achado real novo surgiu durante a implementação (build passou de primeira); o que foi
documentado como "acerto" são decisões já tomadas aqui, registradas pra não serem desfeitas
sem motivo depois (ex. alguém tentar adicionar um listener de orientação ou remover o
`configChanges`).

Também marquei `[x]`/`testada` nos 6 itens da **Fase 2 · TV ao vivo** na §6 do design
(`docs/superpowers/specs/2026-11-10-iptv-player-free-design.md`) — não toquei na tabela de
acompanhamento do próprio plano (`docs/superpowers/plans/2026-11-12-...md`), que é do
controlador.

## Evidência de build/testes

Comando: `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`.

- `BUILD SUCCESSFUL in 2m 29s` (47 tarefas, 13 executadas).
- Suíte completa: **87 testes, 0 falhas, 15 arquivos de teste** (85 existentes + 2 novos de
  `ConfiguracoesViewModelTest`) — contado via XML de `app/build/test-results/testDebugUnitTest/`.
- Avisos de depreciação pré-existentes (`LocalLifecycleOwner`, `TabRow`, `Locale(String,
  String)`) não são desta tarefa — nenhum novo warning introduzido pelas mudanças.

## Arquivos alterados

Commit `e9068c4` (feat):
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/Navegacao.kt` (modificado)
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoTela.kt` (modificado)
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/resumo/ResumoTela.kt` (modificado)
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/configuracoes/ConfiguracoesViewModel.kt` (novo)
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/configuracoes/ConfiguracoesTela.kt` (novo)
- `app/src/test/java/io/github/pagmovel/iptvplayer/ui/configuracoes/ConfiguracoesViewModelTest.kt` (novo)

Commit `936e5fb` (docs):
- `.claude/skills/iptv-tv-ao-vivo/SKILL.md` (novo)
- `docs/superpowers/specs/2026-11-10-iptv-player-free-design.md` (modificado, só §6)

Autoria dos dois commits: `Marcos Ronaldo <marcos.ronaldo@gmail.com>` via variáveis de
ambiente (repositório sem `user.name`/`user.email` local confirmado antes do commit) — sem
linha de co-autoria, conforme pedido no brief. `git config` não foi tocado.

## Autorrevisão

- Paisagem: só vídeo + `Osd` renderizam (Voltar/TabRow/abas condicionados a `!emPaisagem`). Confirmado lendo o diff final do arquivo.
- Retrato: estrutura idêntica à anterior (mesmos componentes, mesma ordem), só envolvidos no `if`.
- Rotação não derruba o `PlayerView`: nenhum `remember`/`key`/`LaunchedEffect` novo usa `emPaisagem` como chave; o único consumo de `emPaisagem` é em `Modifier`/visibilidade condicional, lido a cada recomposição normal. `configChanges` da T6 intacto (não foi tocado).
- `onAbrirConfiguracoes` navega de verdade: `Navegacao.kt` troca `tela` para `Tela.CONFIGURACOES` nos dois pontos de entrada, e `ConfiguracoesTela` existe e lê/grava via `ConfiguracoesRepositorio`.
- "Configurações" acessível pelo OSD (atalho já existente desde T9, agora ligado) e pelo Resumo (botão novo).
- Único caller de `ResumoTela` (`Navegacao.kt`) já atualizado com o novo parâmetro — confirmado por busca no projeto inteiro, sem outro lugar instanciando a tela.
- `.claude/` não está no `.gitignore` (`git check-ignore` não achou nada) — commit da skill nova está seguro.
- Alterações não minhas no diretório (`docs/superpowers/plans/...md`, `.superpowers/sdd/...`, `fwcd.kotlin-0.2.36.vsix`) permanecem intocadas e não commitadas — são do controlador/dono, fora do escopo desta T10.

## Dúvidas/preocupações

Nenhuma. Build e testes passaram de primeira, sem achado real novo durante a implementação.
Um ponto de julgamento registrado (não bloqueante): botão "Salvar" explícito em
`ConfiguracoesTela` em vez de salvar a cada tecla — evita gravar um valor inválido/parcial
enquanto o usuário ainda está digitando (ex. "0" antes de completar "08"). Se o dono preferir
salvar automático, é uma troca pequena e isolada nesse arquivo.
