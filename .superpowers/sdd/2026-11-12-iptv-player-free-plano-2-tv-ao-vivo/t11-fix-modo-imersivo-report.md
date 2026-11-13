# T11 — fix: modo imersivo real na TV ao vivo em paisagem

## Bug corrigido

Reportado pelo dono durante a validação manual (T11) no celular dele, contra o provedor
real: ao deitar o celular na tela de TV ao vivo, o app já escondia a própria UI (Voltar,
TabRow, abas — feito na T10), mas a barra de status do Android (hora, notificações,
sinal/bateria) e a barra de navegação/comandos continuavam visíveis por cima do vídeo.
Esperado: modo imersivo "sticky" padrão do Android — barras do sistema escondidas em tela
cheia, reaparecem com um gesto de deslizar e escondem de novo sozinhas.

## O que foi implementado

Em `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoTela.kt`, logo após o
cálculo de `telaCheia` (já existente, T10, computado de `emPaisagem && canalAtual != null`):

```kotlin
val view = LocalView.current
DisposableEffect(telaCheia) {
    val window = (view.context as Activity).window
    val controller = WindowCompat.getInsetsController(window, view)
    if (telaCheia) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
    onDispose {
        controller.show(WindowInsetsCompat.Type.systemBars())
        WindowCompat.setDecorFitsSystemWindows(window, true)
    }
}
```

- `LocalView.current` + `(view.context as Activity).window` — padrão já usado por libs como
  Accompanist System UI Controller para obter a `Window` dentro de um `@Composable` sem
  depender de lib de navegação (este projeto não usa nenhuma). `view.context` é o `Context`
  da própria `ComposeView` criada pelo `setContent` da `Activity`, então o cast é direto.
- Esconde `WindowInsetsCompat.Type.systemBars()` (status + navegação juntas) e configura
  `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` — é exatamente o comportamento "sticky" pedido
  (reaparece com deslizar, esconde de novo sozinho), diferente do "immersive lean" que não
  reaparece nunca.
- **Única chave do efeito é `telaCheia`** (não precisei de `LaunchedEffect` + `DisposableEffect`
  separados): o próprio `DisposableEffect` cobre as duas transições pedidas —
  - `telaCheia` muda de valor (girou pro retrato, ou canal ficou `null`): o Compose dispara o
    `onDispose` da instância anterior (que tinha `telaCheia == true`) **antes** de rodar o
    corpo novo — restaura as barras aí.
  - o composable inteiro sai de composição (saiu da tela de TV ao vivo direto de paisagem,
    sem passar por retrato primeiro): o mesmo `onDispose` roda na saída.
  Por isso o corpo do efeito só precisa fazer algo quando `telaCheia == true`; o `onDispose`
  sempre restaura, e restaurar quando já estava restaurado é inofensivo (idempotente).

## Onde ficou o `setDecorFitsSystemWindows` e por quê

**Dentro do mesmo `DisposableEffect(telaCheia)` em `AoVivoTela.kt`, condicionado ao mesmo
booleano — não em `MainActivity.onCreate`.** Não precisei tocar em `MainActivity.kt`.

Motivo: uma chamada única e incondicional no `onCreate` faria o conteúdo da `Activity`
inteira (Resumo, Contas, Configurações) ocupar a área das barras do sistema permanentemente,
exigindo padding manual nessas telas pra não ficar por baixo da status bar — eu criaria um
efeito colateral em telas que nunca entram em tela cheia, só pra resolver um problema que só
existe na TV ao vivo em paisagem. Condicionar ao `telaCheia` (liga/desliga junto com o resto)
mantém o resto do app exatamente como estava, e o custo de chamar
`setDecorFitsSystemWindows` repetidamente (entrar/sair de telaCheia) é desprezível — é só
uma flag na `Window`, sem relayout pesado.

## Evidência de build/testes

Comando: `./scripts/gradle.ps1 testDebugUnitTest assembleDebug` (suíte completa, sem
filtro).

- `BUILD SUCCESSFUL in 2m 45s` (47 tarefas: 2 executadas, 45 up-to-date — o grosso do
  trabalho de compilação/teste já tinha rodado numa tentativa anterior que excedeu o timeout
  da ferramenta mas continuou e terminou em segundo plano; esta segunda chamada, já em
  background, só confirmou o resultado e completou o `assembleDebug`).
- Testes: somado os 15 arquivos `TEST-*.xml` em `app/build/test-results/testDebugUnitTest/`
  → **87 testes, 0 falhas, 0 erros** (87/87, igual ao baseline — nenhum teste novo, como
  esperado: lógica de `Window`/`Activity` real não é testável por unidade).
- APK gerado: `app/build/outputs/apk/debug/app-debug.apk` (16.689.315 bytes, timestamp
  13/11/2026 09:33, condizente com este build).

## Arquivos alterados

- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoTela.kt` — único arquivo
  modificado (27 linhas inseridas, nenhuma removida): 4 imports novos (`android.app.Activity`,
  `androidx.compose.ui.platform.LocalView`, `androidx.core.view.WindowCompat`,
  `androidx.core.view.WindowInsetsCompat`, `androidx.core.view.WindowInsetsControllerCompat`)
  e o bloco `DisposableEffect(telaCheia)` descrito acima.
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/MainActivity.kt` — **não alterado**
  (ver seção acima).

## Commit

`926c4e4` — `fix: modo imersivo real na TV ao vivo em paisagem (esconde barras do sistema)`
Autor/committer: `Marcos Ronaldo <marcos.ronaldo@gmail.com>` (via variáveis de ambiente no
comando, `git config` não foi tocado). Sem linha de co-autoria do Claude, conforme pedido.
Só o arquivo `AoVivoTela.kt` foi staged e commitado — nenhum outro arquivo pendente do
repositório foi tocado (ver "Pendências fora do escopo" abaixo).

## Autorrevisão

- **Entrar em paisagem com canal tocando esconde status bar E barra de navegação?** Sim —
  `controller.hide(WindowInsetsCompat.Type.systemBars())` cobre os dois tipos juntos (não só
  um). Reaparecem só com deslizar e escondem de novo sozinhas: sim, é exatamente o que
  `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` faz (comportamento "sticky" documentado da própria
  API `WindowInsetsControllerCompat`, não comportamento inferido por mim).
- **Voltar pro retrato (ou sair do canal) restaura as barras normalmente?** Sim — `telaCheia`
  virando `false` troca a chave do `DisposableEffect`, disparando o `onDispose` da instância
  anterior (`controller.show(...)` + `setDecorFitsSystemWindows(window, true)`) antes do
  corpo novo rodar.
- **Sair da tela de TV ao vivo direto de paisagem (sem passar por retrato) também restaura?**
  Sim — mesmo `onDispose`, disparado quando o composable sai de composição.
- **O resto do app (Resumo, Contas, Configurações) continua com as barras visíveis
  normalmente, sem efeito colateral?** Sim — o efeito só existe dentro de `AoVivoTela`, só
  age quando `telaCheia == true`, e `MainActivity`/as outras telas não foram tocadas.
- **`configChanges="orientation|screenSize|screenLayout"` (T6) continua intacto?** Sim — não
  toquei no manifest; a `Activity` continua não sendo recriada ao girar, então o
  `PlayerView`/`ExoPlayer` (vivos em `remember`) não são afetados por este fix, e o próprio
  `DisposableEffect` novo também não depende de recriação de `Activity` pra funcionar (ele
  reage à recomposição de `telaCheia`, que já é o canal certo de atualização aqui).

## Dúvidas / preocupações

Nenhuma dúvida bloqueante. Único ponto a registrar (não é bloqueio, é característica
conhecida do `WindowInsetsControllerCompat` e não deste fix): a chamada `controller.hide(...)`
é "melhor esforço" — em alguns fabricantes/versões de Android com gestos de sistema
customizados, o SO pode reexibir brevemente uma barra em situações específicas (ex. alerta de
bateria crítica do sistema); isso é comportamento do SO, não do app, e está fora do que
qualquer app pode controlar via API pública. Validação real no celular do dono é o próximo
passo (conforme pedido na task) — autorrevisão de código feita, mas o comportamento visual
exato da transição depende de hardware/Android real, sem emulador nesta máquina.

### Pendências fora do escopo desta tarefa (sinalizadas, não resolvidas)

No repositório havia, antes e depois deste commit, alterações não commitadas que **não são
minhas** e que não toquei (minha tarefa era só o fix do modo imersivo):
- `.superpowers/sdd/2026-11-12-iptv-player-free-plano-2-tv-ao-vivo/progress.md` — modificado.
- `.claude/MEMORY.md` e `.claude/memory/` — não rastreados. Pela regra global do dono
  (`.claude/` do projeto sempre commitado), isso deveria entrar no git, mas não é algo que eu
  decido sozinho nesta tarefa — fica sinalizado pro agente/usuário que despachou este
  trabalho.
- `fwcd.kotlin-0.2.36.vsix` — não rastreado; o próprio `CLAUDE.md` do projeto já registra que
  o dono decidiu deixar como está, sem versionar. Confirmado, não é pendência real.
