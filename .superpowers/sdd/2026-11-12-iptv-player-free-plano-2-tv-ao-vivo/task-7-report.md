# T7: Tela Ao Vivo — aba Canais

## O que foi implementado

- `AoVivoViewModel` (`ui/aovivo/AoVivoViewModel.kt`): resolve a conta ativa e monta o
  `PlayerAoVivo` em `carregar()` (suspend, chamado uma vez pela tela); expõe `categorias`,
  `favoritos` e `canaisFiltrados` (Flows montados uma única vez dentro de `carregar()`, não
  como `get()` computada — ver "Decisões de integração" abaixo); `selecionarCanal`,
  `favoritar`/`desfavoritar`, `filtrarPorCategoria`/`filtrarFavoritos`, `tentarNovamente`,
  `pausar`/`retomar`, `liberar`, `programaAtual(canal)`.
- `CanaisAba` (`ui/aovivo/CanaisAba.kt`): chips Todos/Favoritos/categorias (seleção única),
  lista de `CanalAgrupado` com nome, ícone (sempre "nome sobre fundo neutro — ver decisão
  abaixo), estrela de favorito e o programa atual via `produceState` por linha.
- `AoVivoTela` (`ui/aovivo/AoVivoTela.kt`): `AndroidView`/`PlayerView` ligado a
  `viewModel.exoPlayer`, overlay de erro + "Tentar de novo" só em `FalhaTerminal`, `TabRow`
  Canais/Programação (Programação com placeholder — T8 não existe ainda), observador de
  ciclo de vida (`DisposableEffect` + `LifecycleEventObserver`) chamando
  `pausar`/`retomar`/`liberar`, e um botão "Voltar" (decisão minha, ver abaixo).
- `Navegacao.kt`: `Tela.AO_VIVO` novo, `ResumoTela` ganhou `onAssistirAoVivo`.
- `ResumoTela.kt`: botão "Assistir ao vivo" ao lado de "Gerenciar contas".
- Teste: `AoVivoViewModelTest.kt` (7 casos) cobrindo as duas peças de lógica própria não
  triviais que extraí como funções puras (ver abaixo) — o resto do ViewModel só repassa
  chamadas pra DAO/repositórios/cascata/player, já cobertos pelos testes de T1/T2/T3/T5/T6.

## Decisões de integração

1. **Onde o `PlayerAoVivo` é montado**: dentro de `AoVivoViewModel.carregar()` (suspend),
   chamado de um `LaunchedEffect(viewModel)` em `AoVivoTela` — igual ao padrão de
   `ResumoTela.LaunchedEffect(Unit) { ... }`. Resolve a conta ativa
   (`banco.contaDao().ativa()`), decifra as credenciais (`contasRepositorio.credenciais`),
   monta `api = app.criarApi(cred)` e constrói `PlayerAoVivo(app) { streamId ->
   api.urlAoVivo(streamId) }` (`AppIptv` é `Context`, serve direto). Só roda uma vez (campo
   `lateinit`), como o brief pediu.
2. **Os `Flow`s (`categorias`/`favoritos`/`canaisFiltrados`) são campos `lateinit`
   atribuídos uma única vez dentro de `carregar()`, não propriedades `get()` computadas.**
   Cheguei a desenhar com `get()` (reavaliado a cada acesso), mas isso criaria uma instância
   nova de `Flow` (do `combine(...)`) a cada recomposição que lê a propriedade — o
   `collectAsState` do Compose re-assinaria do zero a cada recomposição em vez de manter a
   mesma inscrição. Campo fixo, montado uma vez, resolve sem precisar de
   `remember`/`flatMapLatest`.
3. **`programaAtual` não usa `inicio < fimMs` sozinho para achar "o que está no ar".** A
   lógica pura que extraí (`programaNoInstante`) checa `inicio <= agora && fim > agora` —
   não só `fim > agora`. Achado real ao escrever o teste: com `lastOrNull { it.fim > agora
   }` sozinho, uma lista com um programa futuro (`inicio > agora`) no fim da lista "vence"
   por estar depois na ordem, mesmo sem ainda ter começado. Na consulta real
   (`programacao(contaId, epgId, agora-24h, agora+1)`) isso não aconteceria (a query já
   filtra `inicio < agora+1`), mas a função pura ficou correta independente de quem chama,
   em vez de depender da janela de quem chama estar sempre certa.
4. **Ícone do canal: sempre "nome sobre fundo neutro", nunca carrega `canal.icone` de
   verdade.** *(ponytail, decisão minha, comentário no código em `CanaisAba.kt`)* — o
   projeto não tem Coil/Glide nem nenhuma lib de imagem ainda, e adicionar uma só para isto
   seria escopo novo fora do brief. O próprio brief permite esse fallback ("ícone (ou nome
   sobre fundo neutro, mesma regra do design)"), então usei sempre o fallback. Troca por
   `AsyncImage` fácil quando uma lib de imagem entrar no projeto.
5. **Chips Todos/Favoritos/categoria são seleção única** (design: "chips de categoria
   (incluindo Favoritos e Todos)"), não toggles independentes — escolher uma reseta as
   outras duas. `AoVivoViewModel.filtrarPorCategoria`/`filtrarFavoritos` continuam expostos
   como ações independentes (o brief pediu assim); quem garante a exclusividade é a UI
   (`CanaisAba`), chamando os dois setters juntos a cada clique de chip.
6. **Botão "Voltar" em `AoVivoTela`** — não estava no brief (que só define o `TabRow` e o
   player), mas sem ele a tela ficaria sem saída (o OSD completo do design, com
   voltar/sair, é escopo de uma tarefa futura). Mesmo padrão simples de `TextButton` que
   `ContasTela`/`ResumoTela` já usam.
7. **Badge "Mudou para HD" do design não foi implementado** — o brief pede só o overlay de
   `FalhaTerminal` com "Tentar de novo"; o selo de troca automática (evento `Trocou`) é
   extra do design não listado nos passos do brief. Deixei de fora (ponytail/YAGNI) — fácil
   de acrescentar depois com um `Snackbar`/toast reagindo a `eventoAtual`.
8. **Aba "Programação" é um placeholder de texto** ("Programação chega na T8.") — T8 não
   existe ainda; o `TabRow` já liga certo pra quando ela chegar.

## Evidência de build/testes

Comando: `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`.

- 1ª rodada: `AoVivoViewModelTest > programaNoInstante acha o programa cujo fim ainda nao
  passou FAILED` (74 testes, 1 falha) — bug real na minha primeira versão de
  `programaNoInstante` (só `fim > agora`, sem checar `inicio <= agora`), achado pelo próprio
  teste que escrevi. Corrigido (item 3 acima).
- 2ª rodada: **`BUILD SUCCESSFUL`**, **74 testes, 0 falhas** (67 pré-existentes + 7 novos em
  `AoVivoViewModelTest`), `assembleDebug` concluído.
- Confirmado por contagem real nos XML de `app/build/test-results/testDebugUnitTest/`: soma
  de `tests=` entre os 14 arquivos = 74, soma de `failures=` = 0.
- Avisos de depreciação (não erros): `LocalLifecycleOwner` (compose-ui) e `TabRow`
  (material3) têm substitutos mais novos (`lifecycle-runtime-compose`,
  `PrimaryTabRow`/`SecondaryTabRow`); usei as versões atuais porque são as já presentes nas
  dependências do projeto (sem adicionar lib nova) — não é bloqueio.

## Arquivos alterados

- Novo: `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoViewModel.kt`
- Novo: `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoTela.kt`
- Novo: `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/CanaisAba.kt`
- Novo: `app/src/test/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoViewModelTest.kt`
- Modificado: `app/src/main/java/io/github/pagmovel/iptvplayer/ui/Navegacao.kt`
- Modificado: `app/src/main/java/io/github/pagmovel/iptvplayer/ui/resumo/ResumoTela.kt`

## Autorrevisão (perguntas do brief)

- **Os 7 passos do brief foram implementados?** Sim.
- **`AoVivoTela` mostra erro + "Tentar de novo" só em `FalhaTerminal`, nunca tenta
  sozinha?** Sim — o overlay só aparece quando `viewModel.eventoAtual ==
  EventoCascata.FalhaTerminal`, e a única chamada a `tentarNovamente()` é o `onClick` desse
  botão.
- **Vídeo para em `ON_STOP` e reabre do zero em `ON_START`?** Sim — `pausar()` chama
  `exoPlayer.pause()` (não libera nada); `retomar()` chama `selecionarCanal(canalAtual)` de
  novo, que cria uma `CascataQualidade` nova e reabre o `MediaItem` do zero (não é retomar
  posição).
- **`ON_DISPOSE` libera o player de verdade?** Sim — o `onDispose` do `DisposableEffect`
  chama `viewModel.liberar()`, que chama `playerAoVivo.liberar()` (cancela o escopo interno
  do `PlayerAoVivo` e `exoPlayer.release()`) e cancela o escopo próprio do ViewModel.
- **A navegação segue o padrão exato de `Navegacao.kt`?** Sim — mesmo `when` sobre um
  `enum class Tela` privado, mesma forma de `remember { ViewModel(...) }` por tela.
- **`tempoBufferMs()` é lido a cada `selecionarCanal`, não uma vez só?** Sim —
  `app.configuracoes.tempoBufferMs()` é chamado dentro de `selecionarCanal`, ao montar cada
  `CascataQualidade` nova.
- **`categoriaId` normalizado antes de favoritar/desfavoritar?** Sim, e sem normalização
  extra: `CanalAgrupado.categoriaId` já é o valor normalizado por `chaveCategoria` (contrato
  de T1 — `agruparPorQualidade` monta o `CanalAgrupado` com `categoriaId = chaveCategoria(...)`),
  então `favoritar`/`desfavoritar` em `AoVivoViewModel` usam `canal.categoriaId` direto, sem
  risco de passar um `Long?` cru.

## Dúvidas/preocupações

- Itens 6–8 de "Decisões de integração" são escopo que acrescentei ou deixei de fora por
  decisão própria (ponytail/YAGNI), não pedidos explícitos do brief — sinalizando para
  quem revisar, caso o dono prefira o OSD completo ou o selo de troca já nesta tarefa.
- Esta tarefa não tem teste automatizado de Compose (mesmo padrão do Plano 1 — ver T11).
  Validação real da UI (chips, estrela, cascata visível na tela, pausa/retomada ao trocar
  de app) depende de teste manual no celular.
- Fora do escopo desta tarefa, mas notado: havia alterações não commitadas de outras
  tarefas/arquivos no repositório antes de eu começar (`docs/superpowers/plans/....md`
  modificado, e vários arquivos em `.superpowers/sdd/.../` de T1–T6 — briefs, reports,
  `progress.md`, `plan-path`, diffs de review — ainda não commitados). Não toquei neles;
  meu commit inclui só os arquivos de T7 (código + este relatório).
