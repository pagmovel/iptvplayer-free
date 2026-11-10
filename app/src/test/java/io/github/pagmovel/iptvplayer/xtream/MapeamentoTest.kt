package io.github.pagmovel.iptvplayer.xtream

import io.github.pagmovel.iptvplayer.dados.Canal
import io.github.pagmovel.iptvplayer.dados.Categoria
import io.github.pagmovel.iptvplayer.dados.Filme
import io.github.pagmovel.iptvplayer.dados.Serie
import io.github.pagmovel.iptvplayer.dados.TipoConteudo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapeamentoTest {

    private fun obj(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    @Test fun `numero como texto e como numero`() {
        assertEquals(42L, obj("""{"stream_id":"42"}""").long("stream_id"))
        assertEquals(42L, obj("""{"stream_id":42}""").long("stream_id"))
    }

    @Test fun `vazio nulo e ausente viram null`() {
        val o = obj("""{"rating":"","category_id":null}""")
        assertNull(o.decimal("rating"))
        assertNull(o.long("category_id"))
        assertNull(o.texto("nao_existe"))
    }

    @Test fun `canal sem categoria e sem icone ainda e importado`() {
        val c = obj(
            """{"stream_id":7,"name":"Canal Sete","category_id":null,"stream_icon":"","epg_channel_id":"sete.br","num":3}""",
        ).paraCanal(1)!!
        assertEquals(
            Canal(contaId = 1, streamId = 7, nome = "Canal Sete", categoriaId = null, icone = null, epgId = "sete.br", numero = 3),
            c,
        )
    }

    @Test fun `filme le extensao ano e nota`() {
        val f = obj(
            """{"stream_id":10,"name":"Filme Dez","category_id":5,"container_extension":"mkv","year":"2019","rating":"7.5"}""",
        ).paraFilme(1)!!
        assertEquals(
            Filme(
                contaId = 1, streamId = 10, nome = "Filme Dez", categoriaId = 5, capa = null,
                extensao = "mkv", ano = 2019, nota = 7.5, adicionadoEm = null,
            ),
            f,
        )
    }

    @Test fun `serie le capa sinopse e ultima modificacao`() {
        val s = obj(
            """{"series_id":20,"name":"Serie Vinte","category_id":8,"cover":"http://capa.jpg","plot":"Sinopse","last_modified":"1700000000"}""",
        ).paraSerie(1)!!
        assertEquals(
            Serie(
                contaId = 1, seriesId = 20, nome = "Serie Vinte", categoriaId = 8, capa = "http://capa.jpg",
                sinopse = "Sinopse", ano = null, nota = null, modificadaEm = 1700000000L,
            ),
            s,
        )
    }

    @Test fun `categoria le id e nome`() {
        val cat = obj("""{"category_id":"9","category_name":"Esportes"}""").paraCategoria(1, TipoConteudo.AO_VIVO)!!
        assertEquals(Categoria(contaId = 1, tipo = TipoConteudo.AO_VIVO, categoriaId = 9, nome = "Esportes"), cat)
    }

    @Test fun `item sem id ou sem nome devolve null`() {
        assertNull(obj("""{"name":"Sem id"}""").paraCanal(1))
        assertNull(obj("""{"stream_id":1}""").paraCanal(1))
        assertNull(obj("""{"name":"Sem id"}""").paraFilme(1))
        assertNull(obj("""{"stream_id":1}""").paraFilme(1))
        assertNull(obj("""{"name":"Sem id"}""").paraSerie(1))
        assertNull(obj("""{"series_id":1}""").paraSerie(1))
        assertNull(obj("""{"category_name":"Sem id"}""").paraCategoria(1, TipoConteudo.AO_VIVO))
        assertNull(obj("""{"category_id":1}""").paraCategoria(1, TipoConteudo.AO_VIVO))
    }
}
