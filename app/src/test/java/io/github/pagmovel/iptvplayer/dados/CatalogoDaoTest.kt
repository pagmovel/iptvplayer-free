package io.github.pagmovel.iptvplayer.dados

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CatalogoDaoTest {

    private lateinit var banco: BancoApp
    private lateinit var dao: CatalogoDao

    @Before
    fun subirBanco() {
        banco = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), BancoApp::class.java)
            .allowMainThreadQueries()
            .build()
        dao = banco.catalogoDao()
    }

    @After
    fun fecharBanco() {
        banco.close()
    }

    // streamId/seriesId identificam o item dentro de uma conta; contaId default = 1 porque
    // a maioria dos testes só precisa de uma conta.
    private fun canal(streamId: Long, nome: String, contaId: Long = 1): Canal =
        Canal(contaId = contaId, streamId = streamId, nome = nome, categoriaId = null, icone = null, epgId = null, numero = null)

    private fun filme(streamId: Long, nome: String, contaId: Long = 1): Filme =
        Filme(
            contaId = contaId, streamId = streamId, nome = nome, categoriaId = null, capa = null,
            extensao = null, ano = null, nota = null, adicionadoEm = null,
        )

    private fun serie(seriesId: Long, nome: String, contaId: Long = 1): Serie =
        Serie(
            contaId = contaId, seriesId = seriesId, nome = nome, categoriaId = null, capa = null,
            sinopse = null, ano = null, nota = null, modificadaEm = null,
        )

    @Test fun `busca ignora acento e maiusculas`() = runTest {
        dao.inserirCanais(listOf(canal(1, "São Paulo HD"), canal(2, "Rio FHD")))
        assertEquals(listOf("São Paulo HD"), dao.buscarNomesCanais(contaId = 1, termo = "sao paulo").map { it.nome })
        assertEquals(listOf("São Paulo HD"), dao.buscarNomesCanais(1, "SÃO").map { it.nome })
    }

    @Test fun `busca por prefixo enquanto digita`() = runTest {
        dao.inserirCanais(listOf(canal(1, "São Paulo HD"), canal(2, "Rio FHD")))
        assertEquals(listOf("São Paulo HD"), dao.buscarNomesCanais(1, "pau").map { it.nome })
    }

    @Test fun `contas nao se misturam`() = runTest {
        dao.inserirCanais(listOf(canal(1, "Globo HD", contaId = 1), canal(1, "Globo HD", contaId = 2)))

        assertEquals(listOf("Globo HD"), dao.buscarNomesCanais(1, "globo").map { it.nome })
        assertEquals(1, dao.contagem(1).canais)
        assertEquals(1, dao.contagem(2).canais)
    }

    @Test fun `apagarCatalogo limpa so a conta indicada`() = runTest {
        dao.inserirCanais(listOf(canal(1, "Globo HD", contaId = 1), canal(1, "Globo HD", contaId = 2)))
        dao.inserirFilmes(listOf(filme(1, "Filme A", contaId = 1)))
        dao.inserirSeries(listOf(serie(1, "Serie A", contaId = 1)))

        dao.apagarCatalogo(1)

        assertEquals(Contagem(canais = 0, filmes = 0, series = 0, programas = 0), dao.contagem(1))
        assertEquals(1, dao.contagem(2).canais)
    }

    @Test fun `streamId duplicado na mesma lista nao quebra a insercao`() = runTest {
        // Achado real: o provedor pode mandar o mesmo item duas vezes na mesma resposta de
        // get_vod_streams (stream_id repetido) — sem REPLACE, a segunda inserção lançava
        // SQLiteConstraintException e a carga inteira quebrava sem virar FalhaXtream.
        dao.inserirFilmes(listOf(filme(1, "Filme A (nome antigo)"), filme(1, "Filme A (nome novo)")))

        assertEquals(1, dao.contagem(1).filmes)
    }

    @Test fun `contagem soma cada tipo`() = runTest {
        dao.inserirCategorias(listOf(Categoria(1, TipoConteudo.AO_VIVO, 1, "Esportes")))
        dao.inserirCanais(listOf(canal(1, "Globo HD"), canal(2, "SporTV HD")))
        dao.inserirFilmes(listOf(filme(1, "Filme A")))
        dao.inserirSeries(listOf(serie(1, "Serie A"), serie(2, "Serie B")))
        dao.inserirProgramas(listOf(Programa(1, "epg1", 1000L, 2000L, "Jornal", null)))

        assertEquals(Contagem(canais = 2, filmes = 1, series = 2, programas = 1), dao.contagem(1))
    }

    @Test fun `favoritar e desfavoritar um canal agrupado`() = runTest {
        val favorito = CanalFavorito(contaId = 1, nomeBase = "Globo", categoriaId = -1L, criadoEm = 1000L)

        dao.favoritar(favorito)
        assertEquals(listOf(favorito), dao.favoritos(1).first())

        dao.desfavoritar(contaId = 1, nomeBase = "Globo", categoriaId = -1L)
        assertEquals(emptyList<CanalFavorito>(), dao.favoritos(1).first())
    }
}
