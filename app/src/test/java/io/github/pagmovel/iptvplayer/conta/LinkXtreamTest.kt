package io.github.pagmovel.iptvplayer.conta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkXtreamTest {
    @Test
    fun `extrai do link no formato que o provedor manda`() {
        val texto = "🟢 *Link Dns (M3U) :\n\n http://exemplo.tv:80/get.php?username=joao&password=s3nh4&type=m3u_plus&output=mpegts\n"
        assertEquals(CredenciaisXtream("http://exemplo.tv:80", "joao", "s3nh4"), lerLinkXtream(texto))
    }

    @Test
    fun `aceita link da player_api e sem porta`() {
        assertEquals(CredenciaisXtream("http://exemplo.tv", "a", "b"),
            lerLinkXtream("http://exemplo.tv/player_api.php?username=a&password=b"))
    }

    @Test
    fun `decodifica caracteres codificados na senha`() {
        assertEquals("a+b&c", lerLinkXtream("http://x.tv/get.php?username=u&password=a%2Bb%26c")!!.senha)
    }

    @Test
    fun `devolve null sem usuario ou senha ou sem link`() {
        assertNull(lerLinkXtream("http://x.tv/get.php?username=u"))
        assertNull(lerLinkXtream("texto qualquer"))
        assertNull(lerLinkXtream(""))
    }
}
