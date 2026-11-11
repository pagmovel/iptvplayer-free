package io.github.pagmovel.iptvplayer.carga

import androidx.room.Room
import io.github.pagmovel.iptvplayer.conta.CredenciaisXtream
import io.github.pagmovel.iptvplayer.dados.BancoApp
import io.github.pagmovel.iptvplayer.dados.Contagem
import io.github.pagmovel.iptvplayer.xtream.FalhaXtream
import io.github.pagmovel.iptvplayer.xtream.XtreamApi
import java.time.ZonedDateTime
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Orquestração de ponta a ponta (T9): servidor Xtream fake (MockWebServer) + banco Room em
 * memória (Robolectric, como em [io.github.pagmovel.iptvplayer.dados.CatalogoDaoTest]).
 * Valores de JSON/XML são só formato inventado — nunca dados do provedor real (ver CLAUDE.md).
 */
@RunWith(RobolectricTestRunner::class)
class CargaCatalogoTest {

    private lateinit var servidor: MockWebServer
    private lateinit var banco: BancoApp
    private lateinit var carga: CargaCatalogo

    private val agoraFixo = { ZonedDateTime.parse("2026-11-10T12:00:00Z") }

    @Before
    fun montar() {
        servidor = MockWebServer()
        servidor.start()
        val cred = CredenciaisXtream(servidor.url("/").toString().removeSuffix("/"), "u", "p")
        val api = XtreamApi(OkHttpClient(), cred)
        banco = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), BancoApp::class.java)
            .allowMainThreadQueries()
            .build()
        carga = CargaCatalogo(api, banco, contaId = 1, agora = agoraFixo)
    }

    @After
    fun desmontar() {
        banco.close()
        servidor.shutdown()
    }

    /** Captura a falha lançada pelo bloco e confirma o tipo exato, ou falha o teste (mesmo
     * padrão de XtreamApiTest, T5). */
    private suspend inline fun <reified T : FalhaXtream> esperarFalha(crossinline bloco: suspend () -> Unit): T {
        try {
            bloco()
        } catch (e: Throwable) {
            if (e is T) return e
            throw e
        }
        throw AssertionError("deveria ter lançado ${T::class.simpleName}")
    }

    private fun canal(streamId: Int) = """{"stream_id":$streamId,"name":"Canal $streamId"}"""
    private fun filme(streamId: Int) = """{"stream_id":$streamId,"name":"Filme $streamId"}"""
    private fun serie(seriesId: Int) = """{"series_id":$seriesId,"name":"Serie $seriesId"}"""
    private fun lista(vararg itens: String) = MockResponse().setBody("[${itens.joinToString(",")}]")

    private fun auth() = MockResponse().setBody(
        """{"user_info":{"auth":1,"status":"Active","exp_date":"1800000000","max_connections":"1"}}""",
    )

    private fun programa(inicio: String, fim: String, titulo: String) =
        """<programme start="$inicio" stop="$fim" channel="ch"><title>$titulo</title></programme>"""

    private fun xmltv(vararg programas: String) = MockResponse().setBody("<tv>${programas.joinToString("")}</tv>")

    /** Enfileira uma carga completa e bem-sucedida: autenticação, as 3 categorias (vazias —
     * a contagem não verifica categoria) e os canais/filmes/séries/programas dados. */
    private fun enfileirarCargaOk(
        canais: List<String> = emptyList(),
        filmes: List<String> = emptyList(),
        series: List<String> = emptyList(),
        programas: List<String> = emptyList(),
    ) {
        servidor.enqueue(auth())
        servidor.enqueue(lista()) // get_live_categories
        servidor.enqueue(lista()) // get_vod_categories
        servidor.enqueue(lista()) // get_series_categories
        servidor.enqueue(lista(*canais.toTypedArray()))
        servidor.enqueue(lista(*filmes.toTypedArray()))
        servidor.enqueue(lista(*series.toTypedArray()))
        servidor.enqueue(xmltv(*programas.toTypedArray()))
    }

    @Test fun `carga completa grava tudo e informa as etapas`() = runTest {
        enfileirarCargaOk(
            canais = listOf(canal(1), canal(2), canal(3)),
            filmes = listOf(filme(1), filme(2)),
            series = listOf(serie(1)),
            programas = listOf(
                programa("20261110030000 +0000", "20261110040000 +0000", "P1"),
                programa("20261110040000 +0000", "20261110050000 +0000", "P2"),
                programa("20261111030000 +0000", "20261111040000 +0000", "P3"),
                programa("20261112030000 +0000", "20261112040000 +0000", "P4"),
            ),
        )

        val etapas = mutableListOf<EtapaCarga>()
        val contagem = carga.executar { etapas += it }

        assertEquals(Contagem(canais = 3, filmes = 2, series = 1, programas = 4), contagem)
        assertEquals(EtapaCarga.entries, etapas)
    }

    @Test fun `falha nos filmes mantem o catalogo anterior inteiro`() = runTest {
        enfileirarCargaOk(canais = listOf(canal(1), canal(2), canal(3)))
        carga.executar()

        servidor.enqueue(auth())
        servidor.enqueue(lista()) // get_live_categories
        servidor.enqueue(lista()) // get_vod_categories
        servidor.enqueue(lista()) // get_series_categories
        servidor.enqueue(lista(canal(9)))
        servidor.enqueue(MockResponse().setResponseCode(503)) // get_vod_streams

        esperarFalha<FalhaXtream.ServidorIndisponivel> { carga.executar() }

        assertEquals(3, banco.catalogoDao().contagem(1).canais) // nada pela metade
    }

    @Test fun `senha errada nao apaga nada`() = runTest {
        enfileirarCargaOk(canais = listOf(canal(1), canal(2), canal(3)))
        carga.executar()

        servidor.enqueue(MockResponse().setBody("""{"user_info":{"auth":0}}"""))

        esperarFalha<FalhaXtream.CredenciaisInvalidas> { carga.executar() }

        assertEquals(Contagem(canais = 3, filmes = 0, series = 0, programas = 0), banco.catalogoDao().contagem(1))
    }

    @Test fun `falha so na programacao mantem catalogo novo e programacao antiga`() = runTest {
        enfileirarCargaOk(
            canais = listOf(canal(1), canal(2)),
            programas = listOf(programa("20261110030000 +0000", "20261110040000 +0000", "Antigo")),
        )
        carga.executar()

        servidor.enqueue(auth())
        servidor.enqueue(lista()) // get_live_categories
        servidor.enqueue(lista()) // get_vod_categories
        servidor.enqueue(lista()) // get_series_categories
        servidor.enqueue(lista(canal(1), canal(2), canal(3)))
        servidor.enqueue(lista()) // get_vod_streams
        servidor.enqueue(lista()) // get_series
        servidor.enqueue(MockResponse().setResponseCode(503)) // xmltv.php

        esperarFalha<FalhaXtream.ServidorIndisponivel> { carga.executar() }

        val contagem = banco.catalogoDao().contagem(1)
        assertEquals(3, contagem.canais) // catálogo novo: a transação do catálogo comitou
        assertEquals(1, contagem.programas) // programação antiga: a transação da programação não comitou
    }
}
