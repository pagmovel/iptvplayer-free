package io.github.pagmovel.iptvplayer.configuracoes

import android.content.SharedPreferences

class ConfiguracoesRepositorio(private val prefs: SharedPreferences) {
    private companion object {
        private const val CHAVE_TEMPO_BUFFER_MS = "tempo_buffer_ms"
        private const val TEMPO_BUFFER_PADRAO_MS = 8000L
    }

    fun tempoBufferMs(): Long =
        prefs.getLong(CHAVE_TEMPO_BUFFER_MS, TEMPO_BUFFER_PADRAO_MS)

    fun definirTempoBufferSegundos(segundos: Int) {
        prefs.edit().putLong(CHAVE_TEMPO_BUFFER_MS, segundos * 1000L).apply()
    }
}
