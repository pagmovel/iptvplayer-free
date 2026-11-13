### T6: Player Media3/ExoPlayer

A parte fina que liga `CascataQualidade` (T5) a um `ExoPlayer` de verdade. Não tem suíte
unitária própria de peso (ExoPlayer real não roda bem em JVM puro nem vale a pena fingir) —
validação principal é manual, no celular (T11).

**Arquivos:**
- Criar: `aovivo/PlayerAoVivo.kt`
- Modificar: `gradle/libs.versions.toml`, `app/build.gradle.kts` (dependências Media3),
  `app/src/main/AndroidManifest.xml` (`android:configChanges` na `MainActivity`)

**Interfaces:**
- Consome: `CascataQualidade` (T5), `XtreamApi.urlAoVivo(streamId): String` (já existe, Plano 1).
- Produz: `class PlayerAoVivo(context: Context, private val montarUrl: (Long) -> String)` com
  `val exoPlayer: ExoPlayer` (exposto para o `AndroidView`/`PlayerView` da UI, T7),
  `fun abrir(canal: CanalAgrupado, cascata: CascataQualidade)` (liga os callbacks do
  `Player.Listener` a `cascata.aoTravar()`/`aoEstabilizar()`/`aoErroDefinitivo()`, e observa
  `cascata.eventos` para trocar o `MediaItem` da variante atual), `fun liberar()`.

- [ ] **Passo 1: Adicionar as dependências.** `media3-exoplayer` e `media3-ui`, versão
  `1.11.1`, no `libs.versions.toml` (seguindo o comentário de fonte já usado nas outras
  versões) e em `dependencies { implementation(...) }` no `app/build.gradle.kts`.
- [ ] **Passo 2: Implementar `PlayerAoVivo`.** `Player.Listener.onPlaybackStateChanged`:
  `STATE_BUFFERING` → `cascata.aoTravar()`; `STATE_READY` → `cascata.aoEstabilizar()`.
  `Player.Listener.onPlayerError` → `cascata.aoErroDefinitivo()`. Ao coletar um evento
  `Trocou`/`Tocando` do `cascata.eventos`, troca o `MediaItem` para
  `montarUrl(variante.streamId)` e chama `exoPlayer.setMediaItem(...)` +
  `prepare()` + `play()`.
- [ ] **Passo 3: `android:configChanges="orientation|screenSize|screenLayout"` na
  `MainActivity`** (ver Global Constraints).
- [ ] **Passo 4: Rodar `./scripts/gradle.ps1 assembleDebug`.** Esperado: `BUILD SUCCESSFUL`
  (prova que as dependências novas resolvem e o projeto inteiro ainda compila).
- [ ] **Passo 5: Commit.** `feat: player Media3 ligado a cascata de qualidade`

