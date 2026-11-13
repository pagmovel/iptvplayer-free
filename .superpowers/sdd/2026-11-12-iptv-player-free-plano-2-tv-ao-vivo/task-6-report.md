# T6: Player Media3/ExoPlayer — relatório

## O que foi implementado

1. **Dependências Media3 1.11.1** (`gradle/libs.versions.toml`): nova entrada `media3 =
   "1.11.1"` no bloco `[versions]`, com comentário de fonte (`dl.google.com/android/maven2/
   androidx/media3/media3-exoplayer/maven-metadata.xml`, consultado em 2026-11-12, citado no
   cabeçalho do plano) seguindo o mesmo padrão das outras entradas (ex. `ksp`). Aliases
   `androidx-media3-exoplayer` e `androidx-media3-ui` no `[libraries]`.
2. **`app/build.gradle.kts`**: `implementation(libs.androidx.media3.exoplayer)` e
   `implementation(libs.androidx.media3.ui)` no bloco `dependencies`, com comentário
   explicando o uso (ExoPlayer real + `PlayerView` pra T7).
3. **`PlayerAoVivo.kt`** (novo arquivo,
   `app/src/main/java/io/github/pagmovel/iptvplayer/aovivo/PlayerAoVivo.kt`):
   - `class PlayerAoVivo(context: Context, private val montarUrl: (Long) -> String)`.
   - `val exoPlayer: ExoPlayer` criado via `ExoPlayer.Builder(context).build()`, exposto pra
     UI (T7) ligar ao `PlayerView`.
   - `fun abrir(canal: CanalAgrupado, cascata: CascataQualidade)`:
     - Registra um `Player.Listener`: `onPlaybackStateChanged` mapeia
       `STATE_BUFFERING -> cascata.aoTravar()` e `STATE_READY -> cascata.aoEstabilizar()`;
       `onPlayerError` chama `cascata.aoErroDefinitivo()`.
     - Lança uma coroutine (`escopo.launch { cascata.eventos.collect { ... } }`) que reage a
       `EventoCascata.Tocando` (usa `evento.variante` direto) e `EventoCascata.Trocou` (resolve
       a variante buscando em `canal.variantes` pela qualidade `evento.para`, já que o evento
       só carrega `Qualidade`, não a variante com `streamId`); em ambos os casos troca o
       `MediaItem` via `montarUrl(variante.streamId)` + `setMediaItem` + `prepare()` + `play()`.
       `FalhaTerminal` não dispara troca (não tem variante associada — é responsabilidade da
       UI/T7 mostrar o aviso).
     - **A coleta é iniciada ANTES de `cascata.iniciar()`** (última linha da função) —
       pegadinha documentada pelo revisor da T5 (`eventos` é `SharedFlow` sem replay).
   - `escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`: usar o
     dispatcher *immediate* (não um `Dispatchers.Main` comum) é o que garante, na prática, que
     a inscrição no `collect` aconteça de forma síncrona (até a primeira suspensão) quando
     `abrir()` é chamado da thread principal — sem isso, `escopo.launch { ... }` com dispatcher
     comum apenas agenda a coroutine pra rodar depois, e `cascata.iniciar()` (chamado a seguir,
     de forma síncrona) emitiria o primeiro `Tocando` antes de haver inscrito collector,
     perdendo o evento. Confirmei que `kotlinx-coroutines-android` (que fornece esse
     dispatcher em runtime) já está no classpath, transitivo via `androidx.lifecycle`/
     `androidx.room`/`androidx.activity` (`./scripts/gradle.ps1 app:dependencies
     --configuration debugRuntimeClasspath` mostra `kotlinx-coroutines-android:1.9.0`).
   - `fun liberar()`: `escopo.cancel()` + `exoPlayer.release()`.
4. **`AndroidManifest.xml`**: `android:configChanges="orientation|screenSize|screenLayout"`
   adicionado na tag `<activity android:name=".ui.MainActivity" ...>`.

## Saída do `assembleDebug`

```
./scripts/gradle.ps1 assembleDebug
...
BUILD SUCCESSFUL in 4m 21s
39 actionable tasks: 39 executed
```
Nenhum warning novo associado a `PlayerAoVivo.kt`; os warnings impressos são pré-existentes
(`ContasTela.kt`, `ResumoTela.kt`, `sourceSets` do Room) e não relacionados a esta tarefa.

## Suíte completa (`testDebugUnitTest`, sem filtro)

```
./scripts/gradle.ps1 testDebugUnitTest
...
BUILD SUCCESSFUL in 2m 32s
```
Contagem via XML de resultado (`app/build/test-results/testDebugUnitTest/TEST-*.xml`):
**67 testes, 0 falhas.** Nenhum teste novo nesta tarefa (conforme o brief — ExoPlayer real
não roda em JVM puro; validação manual fica pra T11).

## Arquivos alterados

- `gradle/libs.versions.toml` (modificado)
- `app/build.gradle.kts` (modificado)
- `app/src/main/AndroidManifest.xml` (modificado)
- `app/src/main/java/io/github/pagmovel/iptvplayer/aovivo/PlayerAoVivo.kt` (novo)

## Commit

```
9ce147f feat: player Media3 ligado a cascata de qualidade
```
Autor/committer: `Marcos Ronaldo <marcos.ronaldo@gmail.com>` (variáveis de ambiente no
commit, sem tocar `git config`; repositório sem `user.name`/`user.email` locais). Sem linha
de co-autoria do Claude, conforme pedido nesta tarefa.

## Autorrevisão (checklist do brief)

- `onPlaybackStateChanged`: `STATE_BUFFERING -> cascata.aoTravar()`,
  `STATE_READY -> cascata.aoEstabilizar()` — **OK**.
- `onPlayerError -> cascata.aoErroDefinitivo()` — **OK**.
- Troca de `MediaItem` ao receber `Trocou`/`Tocando`, usando `montarUrl(variante.streamId)` +
  `setMediaItem` + `prepare()` + `play()` — **OK**.
- Coleta do `cascata.eventos` inscrita ANTES de `cascata.iniciar()` (pegadinha do replay) —
  **OK**, e reforçada com `Dispatchers.Main.immediate` pra garantir inscrição síncrona antes
  da emissão (ver explicação acima); comentário no código deixa a razão explícita pra quem
  ler depois.
- `android:configChanges` adicionado na `MainActivity` certa (`.ui.MainActivity`) — **OK**.
- `assembleDebug` e suíte (67/0) passam — **OK**, saídas acima.

## Dúvidas/preocupações

Nenhuma dúvida bloqueante. Único ponto que vale registrar pra quem for revisar: a escolha de
`Dispatchers.Main.immediate` (em vez de `Dispatchers.Main` comum) no `escopo` de
`PlayerAoVivo` é deliberada e crítica pra não repetir a pegadinha do replay da T5 — depende de
`abrir()` ser chamado da thread principal (natural, já que `ExoPlayer`/`PlayerView` exigem
isso). Não há teste automatizado cobrindo essa ordem (ExoPlayer real não roda em JVM); a
verificação fica pra T11 (validação manual no celular).

## Correção pós-revisão (achado Importante)

**Achado:** `abrir(canal, cascata)` registrava um `Player.Listener` novo e lançava uma nova
coroutine de coleta de `cascata.eventos` a cada chamada, sem limpar a chamada anterior. Como
o plano já prevê (T7, `AoVivoViewModel.selecionarCanal`) reabrir a MESMA instância de
`PlayerAoVivo` a cada troca de canal — o `ExoPlayer` é caro, não é recriado —, isso deixava o
listener e o coletor antigos vivos: um timer de buffering pendente do canal anterior
(`aoTravar()`, 8s configuráveis) podia expirar depois da troca e emitir
`Trocou`/`FalhaTerminal`, e a coleta antiga (ainda inscrita) trocava o `MediaItem` do player
de volta pra uma variante do canal que o usuário já tinha deixado. Cada troca de canal também
vazava 1 listener + 1 coroutine, liberados só em `liberar()` (fim da sessão da tela).

**Correção em `PlayerAoVivo.kt`:**
- `Player.Listener` agora é registrado uma única vez, no `init` do `PlayerAoVivo`, delegando
  pra um campo mutável `cascataAtual: CascataQualidade?` em vez de capturar a `cascata` da
  chamada de `abrir()`.
- Novo campo `jobColeta: Job?` guarda o `Job` do `escopo.launch` que coleta
  `cascata.eventos`. `abrir()` agora começa com `jobColeta?.cancel()` (cancela a coleta da
  cascata anterior) antes de atualizar `cascataAtual` e lançar o novo coletor.
- Mantida a garantia da T5 (inscrição antes de `cascata.iniciar()`): o novo
  `jobColeta = escopo.launch { ... }` continua vindo antes de `cascata.iniciar()`, usando o
  mesmo `Dispatchers.Main.immediate` já documentado no código.

**Verificação:**
```
./scripts/gradle.ps1 assembleDebug
...
BUILD SUCCESSFUL in 1m 9s
```
```
./scripts/gradle.ps1 testDebugUnitTest
...
BUILD SUCCESSFUL in 1m 31s
```
Contagem via XML: **67 testes, 0 falhas** (nenhum teste novo — este arquivo continua sem
suíte automatizada própria, como já era o caso antes da correção).

**Commit:** `0e7a767 fix: abrir() limpa listener e coleta da cascata anterior antes de trocar
de canal`.

Os 3 achados Minor da revisão (invariante de `first{}` sem doc; `context` vs
`applicationContext`; contrato "main thread" só em comentário) não foram tratados nesta
correção — ficam deferred no ledger, conforme instrução do controlador.
