package io.github.pagmovel.iptvplayer.ui.configuracoes

import android.content.SharedPreferences
import io.github.pagmovel.iptvplayer.configuracoes.ConfiguracoesRepositorio
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Teste minimo da conversao ms<->segundos (unica logica nao trivial deste ViewModel --
 * o resto e so delegar pra [ConfiguracoesRepositorio], ja testado). */
@RunWith(RobolectricTestRunner::class)
class ConfiguracoesViewModelTest {
    private lateinit var viewModel: ConfiguracoesViewModel

    @Before
    fun montar() {
        val prefs: SharedPreferences = RuntimeEnvironment.getApplication().getSharedPreferences("config", 0)
        prefs.edit().clear().apply()
        viewModel = ConfiguracoesViewModel(ConfiguracoesRepositorio(prefs))
    }

    @Test
    fun `tempoBufferSegundos comeca em 8 por padrao`() {
        assertEquals(8, viewModel.tempoBufferSegundos())
    }

    @Test
    fun `definirTempoBufferSegundos muda o valor lido depois`() {
        viewModel.definirTempoBufferSegundos(12)
        assertEquals(12, viewModel.tempoBufferSegundos())
    }
}
