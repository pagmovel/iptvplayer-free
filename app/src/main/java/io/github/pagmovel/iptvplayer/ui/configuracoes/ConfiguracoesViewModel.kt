package io.github.pagmovel.iptvplayer.ui.configuracoes

import io.github.pagmovel.iptvplayer.configuracoes.ConfiguracoesRepositorio

/**
 * Estado e ações da tela de Configurações (brief T10) — mesma observação de
 * [io.github.pagmovel.iptvplayer.ui.contas.ContasViewModel] sobre não ser um
 * `androidx.lifecycle.ViewModel`. Único campo por enquanto: tempo de buffer (em segundos)
 * antes da troca automática de qualidade (T4/T5).
 */
class ConfiguracoesViewModel(private val repositorio: ConfiguracoesRepositorio) {
    fun tempoBufferSegundos(): Int = (repositorio.tempoBufferMs() / 1000L).toInt()

    fun definirTempoBufferSegundos(segundos: Int) = repositorio.definirTempoBufferSegundos(segundos)
}
