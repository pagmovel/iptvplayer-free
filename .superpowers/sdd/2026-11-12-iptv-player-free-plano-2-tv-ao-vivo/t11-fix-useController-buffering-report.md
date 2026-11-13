# T11 — fix: useController=false e spinner de buffering

Dois bugs reais reportados pelo dono na validação manual (T11) da TV ao vivo, no celular, contra
o provedor real. Causa raiz de ambos já confirmada no código antes de eu começar (não foi
suposição desta sessão): print real do controle nativo do Media3 (Bug 1) e leitura de
`AoVivoTela.kt`/`PlayerAoVivo.kt` mostrando que não existe nenhum estado de buffering exposto
além do spinner de carregamento inicial da tela (Bug 2).

## Bug 1 — controle nativo do ExoPlayer aparecia em vez do nosso OSD

**Causa raiz confirmada**: `PlayerView(context).apply { player = ...; keepScreenOn = true }` em
`AoVivoTela.kt` nunca desativava `useController`, que é `true` por padrão no Media3. Ao tocar na
tela, o `PlayerView` mostrava seu próprio overlay (retroceder/avançar 10s, play/pause, item
anterior/próximo de *playlist*, barra de progresso) em vez do nosso `Osd` — que é quem
efetivamente implementa canal anterior/próximo e seletor de qualidade (o controle nativo nem
tem esse conceito).

**Fix**: adicionei `useController = false` dentro do mesmo `.apply { }` do `PlayerView`, com
comentário explicando o motivo (arquivo `AoVivoTela.kt`, bloco do `AndroidView`).

**Risco verificado antes de mexer**: li `Osd.kt` inteiro antes de desativar `useController`,
como o prompt pedia. Confirmei que `Osd` é só `Column`/`Row`/`TextButton` de texto — não reusa
nem depende de nenhuma parte do controle nativo do Media3. Desativar `useController` não remove
nada que o dono via e gostava; remove só o overlay concorrente que o Media3 desenhava sozinho.

## Bug 2 — sem indicador visual durante buffering

**Causa raiz confirmada**: existe só 1 `CircularProgressIndicator` em `AoVivoTela.kt`, usado só
para o carregamento INICIAL da tela (antes de `viewModel.carregar()` terminar). Nada reagia ao
estado de buffering do player depois disso — nem `PlayerView` tinha `setShowBuffering`
configurado (não configurei isso: optei pelo caminho (a) do prompt, StateFlow próprio, que já
cobre os dois casos pedidos sem depender de UI nativa do Media3 — que o Bug 1 está desativando).

**Onde ficou o estado de buffering, e por quê**: optei pela opção (a) sugerida no prompt —
expor um `StateFlow<Boolean>` em `PlayerAoVivo` (não em `CascataQualidade` nem como listener
próprio na UI), pelos seguintes motivos:

- O listener (`Player.Listener.onPlaybackStateChanged`) que decide `STATE_BUFFERING`/
  `STATE_READY` já existe em `PlayerAoVivo` e já aciona `cascata.aoTravar()`/`aoEstabilizar()`
  nesses exatos pontos — bastou adicionar `_bufferizando.value = true/false` ao lado das
  chamadas já existentes, sem listener novo e sem mudar a assinatura de nada.
- `CascataQualidade` não tem acesso ao `ExoPlayer` real (é só máquina de estados com timer) —
  colocar o estado lá exigiria `PlayerAoVivo` empurrar informação pra cascata só pra UI ler de
  volta, uma volta desnecessária.
- `AoVivoViewModel` já faz exatamente essa ponte pra `exoPlayer` (`val exoPlayer: ExoPlayer
  get() = playerAoVivo.exoPlayer`) — repeti o mesmo padrão pra `bufferizando`.

Implementação (`PlayerAoVivo.kt`):

```kotlin
private val _bufferizando = MutableStateFlow(false)
val bufferizando: StateFlow<Boolean> = _bufferizando.asStateFlow()

// no Player.Listener já existente:
Player.STATE_BUFFERING -> { _bufferizando.value = true; cascataAtual?.aoTravar() }
Player.STATE_READY -> { _bufferizando.value = false; cascataAtual?.aoEstabilizar() }
```

Cobertura dos dois casos pedidos:
- **Troca de canal**: `abrir()` chama `setMediaItem`/`prepare`/`play` a partir do evento
  `Tocando` inicial da cascata — isso dispara `STATE_BUFFERING` no listener normalmente (o
  player estava `IDLE`/parado antes).
- **Troca de qualidade da cascata**: mesmo caminho, via evento `Trocou` — a pegadinha do
  `prepare()` chamado enquanto já em `STATE_BUFFERING` (documentada na skill) não afeta este
  flag: se o player já estava bufferizando, o flag já era `true` e permanece `true` (não há
  gap); se estava `READY` (troca manual com vídeo rodando liso), a transição por `READY` →
  `IDLE` → `BUFFERING` dispara o listener normalmente.

**Não aparecer sobre `FalhaTerminal`**: no ramo `else` do `collect` de `abrir()` (o ramo que
roda quando `evento` é `FalhaTerminal`, variante `null`), adicionei `_bufferizando.value =
false` explicitamente, antes de `exoPlayer.stop()` — não dependi de `stop()` disparar
`STATE_READY` (ele não garante isso). Também zerei em `parar()` (ON_STOP), por defesa: evita um
`true` preso caso o app pause durante um buffering e o estado fique visível em alguma
recomposição futura.

`AoVivoViewModel.kt`: repassa com um `get()` simples, mesmo padrão de `exoPlayer`:
```kotlin
val bufferizando: StateFlow<Boolean> get() = playerAoVivo.bufferizando
```

`AoVivoTela.kt`: `val bufferizando by viewModel.bufferizando.collectAsState()` (mesmo padrão já
usado em `CanaisAba.kt` pra `filtroCategoriaAtual`), e um `CircularProgressIndicator` centrado
sobre o vídeo quando `bufferizando == true`, desenhado entre o `AndroidView` do player e o
`Osd` (antes do bloco de `FalhaTerminal`, que corre independente e não é afetado por esse
`if`).

## Teste automatizado — decisão de não escrever um

Decidi NÃO escrever teste novo pro `bufferizando`, por dois motivos conferidos no código antes
de decidir:

1. A lógica é um mapeamento direto: `true` em `STATE_BUFFERING`, `false` em `STATE_READY` e nos
   dois pontos de reset explícito (`FalhaTerminal`, `parar()`). Não tem branch condicional nem
   decisão de negócio — é o mesmo nível de trivialidade das linhas vizinhas
   (`cascataAtual?.aoTravar()`/`aoEstabilizar()`) que também nunca tiveram teste próprio.
2. `PlayerAoVivo` nunca teve teste unitário no projeto (confirmei: não existe
   `PlayerAoVivoTest.kt`, e o listener depende de um `ExoPlayer` real do Media3 — não é
   Robolectric-friendly como `CatalogoDaoTest`/`ConfiguracoesRepositorioTest`, que usam
   Robolectric só pra `Context`/banco/`WorkManager`, nunca pra media). Testar isso exigiria
   infraestrutura nova (fake `Player`/shadow do ExoPlayer) fora de proporção pro tamanho do
   fix.

Bug 1 (useController) de fato não tem teste automatizado possível, como o prompt já antecipava
— é propriedade de `PlayerView`, só verificável no Android real.

## Evidência de build/testes

Comando: `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`

- `BUILD SUCCESSFUL in 2m 34s` (47 actionable tasks: 11 executed, 36 up-to-date).
- Suíte de testes: 87 testes, 0 falhas, 0 erros, em 15 suítes (confirmado somando os XMLs de
  `app/build/test-results/testDebugUnitTest`) — mesmo total de antes (87/87), nenhum teste novo
  adicionado (decisão acima).
- Únicos warnings do `compileDebugKotlin` são de APIs já deprecadas e pré-existentes
  (`LocalLifecycleOwner`, `TabRow`), nada relacionado a este fix.

## Arquivos alterados

- `app/src/main/java/io/github/pagmovel/iptvplayer/aovivo/PlayerAoVivo.kt` — `_bufferizando`/
  `bufferizando: StateFlow<Boolean>`, atualizado no listener existente e zerado em
  `FalhaTerminal`/`parar()`.
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoViewModel.kt` — repassa
  `bufferizando` (mesmo padrão de `exoPlayer`).
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoTela.kt` —
  `useController = false` no `PlayerView`; `CircularProgressIndicator` condicionado a
  `bufferizando` sobreposto ao vídeo.

Commit `681bfc7` (autor/committer `Marcos Ronaldo <marcos.ronaldo@gmail.com>`, via variáveis de
ambiente — repositório sem `user.name`/`user.email` locais, `git config` não foi tocado).

## Autorrevisão (perguntas do prompt)

- **Tocar na tela agora mostra só o nosso `Osd`, nunca o controle nativo do Media3?** Sim —
  `useController = false` desativa o overlay nativo por completo; o único overlay interativo
  que resta é o `Osd` (clicável via `.clickable` do `Box` pai, como já era). Não pude confirmar
  no celular real (sem emulador nesta máquina, conforme `CLAUDE.md`) — fica como verificação
  manual pendente do dono, igual às tarefas anteriores do Plano 2.
- **O spinner aparece tanto numa troca de canal quanto numa queda de qualidade automática?**
  Sim, pelo raciocínio de cobertura acima (ambos passam por `setMediaItem`/`prepare`/`play` a
  partir de um evento da cascata, e o listener de `STATE_BUFFERING` não distingue a origem).
- **O spinner some quando o vídeo volta a tocar (`STATE_READY`)?** Sim, mesmo listener zera o
  flag em `STATE_READY`.
- **O spinner NÃO aparece por cima do aviso de `FalhaTerminal`?** Sim — zerado explicitamente
  no ramo `else` de `abrir()` (o ramo de `FalhaTerminal`), antes do `exoPlayer.stop()`, sem
  depender de o player passar por `STATE_READY` nesse caminho.

## Dúvidas / preocupações

- **Verificação manual real ainda pendente** (sem emulador nesta máquina): os dois fixes
  compilam e passam na suíte, mas só o dono pode confirmar no celular, contra o provedor real,
  que o controle nativo não aparece mais e que o spinner se comporta como esperado em condições
  reais de rede (Wi-Fi ruim, troca de qualidade em cascata).
- **Fora do escopo desta tarefa, achado no código mas não tocado**: a skill `iptv-tv-ao-vivo`
  documenta um `[A VERIFICAR]` existente em `CascataQualidade.escolherManualmente` (resolve por
  `indexOfFirst { it.qualidade == qualidade }`, pode reselecionar uma variante que já falhou
  quando duas variantes do grupo têm a mesma qualidade) — não é um dos dois bugs deste prompt,
  não mexi nele.
- **Alterações não commitadas de outra tarefa, não tocadas neste commit**: ao abrir o
  repositório havia alterações pendentes que não são minhas desta sessão —
  `.superpowers/sdd/2026-11-12-iptv-player-free-plano-2-tv-ao-vivo/progress.md` modificado, e
  não rastreados `.claude/MEMORY.md`, `.claude/memory/`, o diff de review
  `review-db34c75..926c4e4.diff`, `t11-fix-modo-imersivo-report.md` e `fwcd.kotlin-0.2.36.vsix`
  na raiz. Deixei todos de fora do commit (regra global: nunca commitar alteração do usuário
  sem perguntar) — ficam para o dono/orquestrador decidir se entram num commit separado.
