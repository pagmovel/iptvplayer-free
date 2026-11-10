package io.github.pagmovel.iptvplayer

import org.junit.Assert.assertEquals
import org.junit.Test

class EsqueletoTest {
    @Test
    fun `nome do app e pacote estao corretos`() {
        assertEquals("io.github.pagmovel.iptvplayer", BuildConfig.APPLICATION_ID)
    }
}
