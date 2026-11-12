package io.github.pagmovel.iptvplayer.aovivo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/** Eventos que a cascata de qualidade emite para quem estiver tocando o canal (T7: o player). */
sealed class EventoCascata {
    data class Tocando(val variante: VarianteQualidade) : EventoCascata()
    data class Trocou(val de: Qualidade, val para: Qualidade) : EventoCascata()
    object FalhaTerminal : EventoCascata()
}

/**
 * Maquina de estados pura da troca automatica/manual de qualidade de um [CanalAgrupado].
 * Sem Android, sem ExoPlayer -- quem toca o player chama [aoTravar]/[aoEstabilizar]/
 * [aoErroDefinitivo] e reage aos [eventos].
 *
 * `canal.variantes` ja vem ordenada da maior pra menor qualidade (contrato de T1), entao
 * descer uma qualidade e sempre avancar [indiceAtual] em 1; a pior e o ultimo indice.
 */
class CascataQualidade(
    private val canal: CanalAgrupado,
    private val tempoBufferMs: Long,
    private val escopo: CoroutineScope,
) {
    private val fluxoEventos = MutableSharedFlow<EventoCascata>(extraBufferCapacity = 16)
    val eventos: Flow<EventoCascata> = fluxoEventos.asSharedFlow()

    private var indiceAtual = 0
    private var falhouDeFormaTerminal = false
    private var timerJob: Job? = null

    fun iniciar() {
        indiceAtual = 0
        falhouDeFormaTerminal = false
        timerJob?.cancel()
        timerJob = null
        fluxoEventos.tryEmit(EventoCascata.Tocando(varianteAtual()))
    }

    /** Buffering: (re)inicia o timer de [tempoBufferMs]. Chamar de novo reinicia do zero. */
    fun aoTravar() {
        if (falhouDeFormaTerminal) return
        timerJob?.cancel()
        timerJob = escopo.launch {
            delay(tempoBufferMs)
            descerQualidade()
        }
    }

    /** Reproduzindo bem: cancela o timer pendente, sem descer qualidade. */
    fun aoEstabilizar() {
        timerJob?.cancel()
        timerJob = null
    }

    /** Erro definitivo do player: desce na hora, sem esperar o timer. */
    fun aoErroDefinitivo() {
        if (falhouDeFormaTerminal) return
        timerJob?.cancel()
        timerJob = null
        descerQualidade()
    }

    /** Troca manual: muda o ponto de partida sem tocar no timer; a cascata automatica
     * continua valendo a partir daqui, mesmo que tivesse parado numa falha terminal antes. */
    fun escolherManualmente(qualidade: Qualidade) {
        val indice = canal.variantes.indexOfFirst { it.qualidade == qualidade }
        if (indice == -1) return
        indiceAtual = indice
        falhouDeFormaTerminal = false
    }

    /** So tem efeito depois de [EventoCascata.FalhaTerminal]: reinicia da maior qualidade. */
    fun tentarNovamente() {
        if (!falhouDeFormaTerminal) return
        iniciar()
    }

    private fun descerQualidade() {
        timerJob = null
        if (indiceAtual >= canal.variantes.lastIndex) {
            falhouDeFormaTerminal = true
            fluxoEventos.tryEmit(EventoCascata.FalhaTerminal)
            return
        }
        val de = varianteAtual().qualidade
        indiceAtual += 1
        fluxoEventos.tryEmit(EventoCascata.Trocou(de, varianteAtual().qualidade))
    }

    private fun varianteAtual() = canal.variantes[indiceAtual]
}
