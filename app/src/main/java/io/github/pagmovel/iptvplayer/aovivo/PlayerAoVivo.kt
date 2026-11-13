package io.github.pagmovel.iptvplayer.aovivo

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
 *
 * Uma única instância é reaberta a cada troca de canal (T7, `AoVivoViewModel.selecionarCanal`
 * cria uma `CascataQualidade` nova por canal e chama [abrir] de novo na mesma instância -- o
 * `ExoPlayer` é caro, não é recriado). Por isso o `Player.Listener` é registrado uma única vez
 * (delega pra [cascataAtual], atualizado a cada [abrir]) em vez de um listener novo por
 * chamada, e [abrir] cancela a coleta de eventos da cascata anterior antes de abrir a nova
 * (achado real da revisão: sem isso, um timer de buffering pendente do canal anterior pode
 * expirar depois da troca e a coleta antiga, ainda viva, voltar o `MediaItem` pra uma variante
 * do canal que o usuário já deixou).
 */
class PlayerAoVivo(
    context: Context,
    private val montarUrl: (Long) -> String,
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    // Dispatchers.Main.immediate: chamado da thread principal (mesma do ExoPlayer), o launch
    // em abrir() roda sincrono até a primeira suspensão -- precisa disso porque cascata.eventos
    // é um SharedFlow sem replay (achado real da revisão da T5): se a inscrição no collect só
    // acontecer depois de cascata.iniciar() já ter emitido, o primeiro Tocando se perde.
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var cascataAtual: CascataQualidade? = null
    private var jobColeta: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> cascataAtual?.aoTravar()
                    Player.STATE_READY -> cascataAtual?.aoEstabilizar()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                cascataAtual?.aoErroDefinitivo()
            }
        })
    }

    /**
     * Abre [canal] tocando através de [cascata]. Cancela antes de tudo a coleta de eventos da
     * cascata anterior (se houver) -- senão ela continuaria reagindo a timers/erros do canal
     * de onde o usuário já saiu. A coleta da cascata nova precisa estar inscrita ANTES de
     * chamar [CascataQualidade.iniciar] -- por isso o `escopo.launch` vem antes, e
     * `cascata.iniciar()` é a última linha.
     */
    fun abrir(canal: CanalAgrupado, cascata: CascataQualidade) {
        jobColeta?.cancel()
        cascataAtual = cascata

        jobColeta = escopo.launch {
            cascata.eventos.collect { evento ->
                // Revisao final (Importante 9): evento.para ja e a VarianteQualidade exata
                // escolhida pela cascata -- resolver de novo por `first { it.qualidade == ... }`
                // podia acertar uma variante diferente (mesma qualidade duplicada no grupo).
                val variante = when (evento) {
                    is EventoCascata.Tocando -> evento.variante
                    is EventoCascata.Trocou -> evento.para
                    is EventoCascata.FalhaTerminal -> null
                }
                if (variante != null) {
                    exoPlayer.setMediaItem(MediaItem.fromUri(montarUrl(variante.streamId)))
                    exoPlayer.prepare()
                    exoPlayer.play()
                    // Critico 2: toda variante nova comeca "carregando" do ponto de vista da
                    // cascata. Sem isso, quando o timer da cascata vence, o ExoPlayer ja esta
                    // em STATE_BUFFERING (prepare() nao faz nada fora de IDLE no Media3
                    // 1.11.1) e onPlaybackStateChanged nao dispara de novo -- a cascata trava
                    // sem timer pra proxima tentativa. O STATE_READY seguinte cancela este
                    // timer normalmente via aoEstabilizar().
                    cascata.aoTravar()
                } else {
                    // Importante 4: FalhaTerminal nao pode deixar a conexao HTTP aberta atras
                    // do aviso de erro.
                    exoPlayer.stop()
                }
            }
        }

        cascata.iniciar()
    }

    /** Critico 3: para tudo de verdade -- coleta de eventos, timer da cascata e o player.
     * Chamado de `AoVivoViewModel.pausar()` (ON_STOP) pra segundo plano nao poder voltar a
     * tocar sozinho se o timer da cascata vencer com o app minimizado (o coletor de [abrir]
     * reagiria a um `Trocou` e chamaria `play()` de novo). `retomar()` reabre o canal do zero
     * via `selecionarCanal`, entao parar tudo aqui nao precisa ser desfeito depois. */
    fun parar() {
        jobColeta?.cancel()
        jobColeta = null
        cascataAtual?.aoEstabilizar()
        exoPlayer.stop()
    }

    fun liberar() {
        escopo.cancel()
        exoPlayer.release()
    }
}
