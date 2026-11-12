package io.github.pagmovel.iptvplayer.aovivo

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Liga um [ExoPlayer] de verdade a [CascataQualidade] (T5): estado de buffering/pronto/erro
 * do player alimenta a cascata, e as trocas de variante que a cascata decide trocam o
 * [MediaItem] do player. [montarUrl] monta a URL de streaming a partir do streamId de uma
 * variante (ver `XtreamApi.urlAoVivo`, Plano 1). [exoPlayer] é exposto pra UI (T7) ligar ao
 * `PlayerView`/`AndroidView`.
 */
class PlayerAoVivo(
    context: Context,
    private val montarUrl: (Long) -> String,
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    // Dispatchers.Main.immediate: chamado da thread principal (mesma do ExoPlayer), o launch
    // abaixo roda sincrono até a primeira suspensão -- precisa disso porque cascata.eventos é
    // um SharedFlow sem replay (achado real da revisão da T5): se a inscrição no collect só
    // acontecer depois de cascata.iniciar() já ter emitido, o primeiro Tocando se perde.
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Abre [canal] tocando através de [cascata]. A coleta de [CascataQualidade.eventos]
     * precisa estar inscrita ANTES de chamar [CascataQualidade.iniciar] -- por isso o
     * `escopo.launch` vem primeiro, e `cascata.iniciar()` é a última linha.
     */
    fun abrir(canal: CanalAgrupado, cascata: CascataQualidade) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> cascata.aoTravar()
                    Player.STATE_READY -> cascata.aoEstabilizar()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                cascata.aoErroDefinitivo()
            }
        })

        escopo.launch {
            cascata.eventos.collect { evento ->
                val variante = when (evento) {
                    is EventoCascata.Tocando -> evento.variante
                    is EventoCascata.Trocou ->
                        canal.variantes.first { it.qualidade == evento.para }
                    is EventoCascata.FalhaTerminal -> null
                }
                if (variante != null) {
                    exoPlayer.setMediaItem(MediaItem.fromUri(montarUrl(variante.streamId)))
                    exoPlayer.prepare()
                    exoPlayer.play()
                }
            }
        }

        cascata.iniciar()
    }

    fun liberar() {
        escopo.cancel()
        exoPlayer.release()
    }
}
