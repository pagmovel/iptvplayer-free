package io.github.pagmovel.iptvplayer.aovivo

import io.github.pagmovel.iptvplayer.dados.Canal
import org.junit.Assert.assertEquals
import org.junit.Test

class CanalAgrupadoTest {

    @Test
    fun `agrupa variantes do mesmo canal pelo nome-sem-sufixo e categoria`() {
        // 3 Canal sintéticos: "CANAL TESTE FHD", "CANAL TESTE HD", "CANAL TESTE SD", mesma categoriaId.
        val canais = listOf(
            Canal(contaId = 1, streamId = 1, nome = "CANAL TESTE FHD", categoriaId = 10, icone = "icone.png", epgId = "epg1", numero = 1),
            Canal(contaId = 1, streamId = 2, nome = "CANAL TESTE HD", categoriaId = 10, icone = "icone.png", epgId = "epg1", numero = 1),
            Canal(contaId = 1, streamId = 3, nome = "CANAL TESTE SD", categoriaId = 10, icone = "icone.png", epgId = "epg1", numero = 1),
        )

        val resultado = agruparPorQualidade(canais)

        assertEquals(1, resultado.size)
        val agrupado = resultado[0]
        assertEquals("CANAL TESTE", agrupado.nomeBase)
        assertEquals(10, agrupado.categoriaId)
        assertEquals("icone.png", agrupado.icone)
        assertEquals("epg1", agrupado.epgId)
        assertEquals(1, agrupado.numero)
        assertEquals(3, agrupado.variantes.size)
        // variantes ordenadas de maior pra menor: FHD, HD, SD
        assertEquals(Qualidade.FHD, agrupado.variantes[0].qualidade)
        assertEquals(1L, agrupado.variantes[0].streamId)
        assertEquals(Qualidade.HD, agrupado.variantes[1].qualidade)
        assertEquals(2L, agrupado.variantes[1].streamId)
        assertEquals(Qualidade.SD, agrupado.variantes[2].qualidade)
        assertEquals(3L, agrupado.variantes[2].streamId)
    }

    @Test
    fun `canais com nome-base igual em categorias diferentes nao se misturam`() {
        val canais = listOf(
            Canal(contaId = 1, streamId = 1, nome = "CANAL X HD", categoriaId = 1, icone = null, epgId = null, numero = null),
            Canal(contaId = 1, streamId = 2, nome = "CANAL X HD", categoriaId = 2, icone = null, epgId = null, numero = null),
        )

        val resultado = agruparPorQualidade(canais)

        assertEquals(2, resultado.size)
        assertEquals("CANAL X", resultado[0].nomeBase)
        assertEquals(1, resultado[0].categoriaId)
        assertEquals("CANAL X", resultado[1].nomeBase)
        assertEquals(2, resultado[1].categoriaId)
    }

    @Test
    fun `categoriaId nulo normaliza para -1 sem colidir com categoria real -1`() {
        val canais = listOf(
            Canal(contaId = 1, streamId = 1, nome = "CANAL Y HD", categoriaId = null, icone = null, epgId = null, numero = null),
            Canal(contaId = 1, streamId = 2, nome = "CANAL Y HD", categoriaId = -1L, icone = null, epgId = null, numero = null),
        )

        val resultado = agruparPorQualidade(canais)

        // Ambos normalizam pra -1L, então viram 1 CanalAgrupado só
        assertEquals(1, resultado.size)
        assertEquals("CANAL Y", resultado[0].nomeBase)
        assertEquals(-1L, resultado[0].categoriaId)
        assertEquals(2, resultado[0].variantes.size)
    }

    @Test
    fun `canal sem sufixo de qualidade reconhecido vira grupo de 1 variante`() {
        val canais = listOf(
            Canal(contaId = 1, streamId = 1, nome = "CANAL SEM SUFIXO", categoriaId = 10, icone = null, epgId = null, numero = null),
        )

        val resultado = agruparPorQualidade(canais)

        assertEquals(1, resultado.size)
        val agrupado = resultado[0]
        assertEquals("CANAL SEM SUFIXO", agrupado.nomeBase)
        assertEquals(1, agrupado.variantes.size)
        // Canal sem sufixo reconhecido entra como Qualidade.SD (pior caso)
        assertEquals(Qualidade.SD, agrupado.variantes[0].qualidade)
        assertEquals(1L, agrupado.variantes[0].streamId)
    }

    @Test
    fun `qualidadeDoNome reconhece os 4 sufixos, case-insensitive`() {
        assertEquals(Qualidade.SD, qualidadeDoNome("X SD"))
        assertEquals(Qualidade.SD, qualidadeDoNome("x sd"))
        assertEquals(Qualidade.HD, qualidadeDoNome("X HD"))
        assertEquals(Qualidade.HD, qualidadeDoNome("x hd"))
        assertEquals(Qualidade.FHD, qualidadeDoNome("X FHD"))
        assertEquals(Qualidade.FHD, qualidadeDoNome("x fhd"))
        assertEquals(Qualidade.QUATRO_K, qualidadeDoNome("X 4K"))
        assertEquals(Qualidade.QUATRO_K, qualidadeDoNome("x 4k"))
        // Sem sufixo reconhecido
        assertEquals(null, qualidadeDoNome("X SEM SUFIXO"))
    }
}
