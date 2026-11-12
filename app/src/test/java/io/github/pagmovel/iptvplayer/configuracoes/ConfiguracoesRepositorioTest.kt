package io.github.pagmovel.iptvplayer.configuracoes

import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ConfiguracoesRepositorioTest {
    private lateinit var prefs: SharedPreferences
    private lateinit var repo: ConfiguracoesRepositorio

    @Before
    fun montar() {
        prefs = RuntimeEnvironment.getApplication().getSharedPreferences("config", 0)
        prefs.edit().clear().apply()
        repo = ConfiguracoesRepositorio(prefs)
    }

    @Test
    fun `tempoBufferMs comeca em 8000 (8 segundos) por padrao`() {
        assertEquals(8000L, repo.tempoBufferMs())
    }

    @Test
    fun `definirTempoBufferSegundos muda o valor lido depois`() {
        repo.definirTempoBufferSegundos(5)
        assertEquals(5000L, repo.tempoBufferMs())

        repo.definirTempoBufferSegundos(10)
        assertEquals(10000L, repo.tempoBufferMs())
    }
}
