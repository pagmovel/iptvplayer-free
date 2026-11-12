package io.github.pagmovel.iptvplayer.xtream

import io.github.pagmovel.iptvplayer.conta.CredenciaisXtream
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.concurrent.TimeUnit

class XtreamApiTest {

    private lateinit var servidor: MockWebServer
    private lateinit var api: XtreamApi

    @Before
    fun montar() {
        servidor = MockWebServer()
        servidor.start()
        val cred = CredenciaisXtream(servidor.url("/").toString().removeSuffix("/"), "u", "p")
        api = XtreamApi(OkHttpClient(), cred)
    }

    @After
    fun desmontar() {
        servidor.shutdown()
    }

    /** Captura a falha lançada pelo bloco e confirma o tipo exato, ou falha o teste. */
    private suspend inline fun <reified T : FalhaXtream> esperarFalha(crossinline bloco: suspend () -> Unit): T {
        try {
            bloco()
        } catch (e: Throwable) {
            if (e is T) return e
            throw e
        }
        throw AssertionError("deveria ter lançado ${T::class.simpleName}")
    }

    @Test
    fun `autenticar le conta ativa`() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """{"user_info":{"auth":1,"status":"Active","exp_date":"1800000000","max_connections":"2"}}""",
            ),
        )

        val info = api.autenticar()

        assertEquals(XtreamApi.InfoConta(true, 1800000000L, 2), info)
        assertEquals("/player_api.php?username=u&password=p", servidor.takeRequest().path)
    }

    @Test
    fun `auth 0 vira CredenciaisInvalidas`() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"user_info":{"auth":0}}"""))

        esperarFalha<FalhaXtream.CredenciaisInvalidas> { api.autenticar() }
    }

    @Test
    fun `status Expired vira ContaInativa`() = runTest {
        servidor.enqueue(
            MockResponse().setBody(
                """{"user_info":{"auth":1,"status":"Expired","exp_date":"1800000000","max_connections":"2"}}""",
            ),
        )

        val falha = esperarFalha<FalhaXtream.ContaInativa> { api.autenticar() }

        assertEquals("Expired", falha.status)
    }

    @Test
    fun `HTTP 503 e timeout viram ServidorIndisponivel`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(503))
        esperarFalha<FalhaXtream.ServidorIndisponivel> { api.autenticar() }

        // Cliente com leitura bem curta para não deixar o teste lento: o corpo nunca chega
        // a tempo e vira IOException antes mesmo do callTimeout (20s) de autenticar().
        servidor.enqueue(MockResponse().setHeadersDelay(2, TimeUnit.SECONDS))
        val credLenta = CredenciaisXtream(servidor.url("/").toString().removeSuffix("/"), "u", "p")
        val apiLenta = XtreamApi(
            OkHttpClient.Builder().readTimeout(200, TimeUnit.MILLISECONDS).build(),
            credLenta,
        )
        esperarFalha<FalhaXtream.ServidorIndisponivel> { apiLenta.autenticar() }
    }

    @Test
    fun `resposta HTML vira RespostaInvalida`() = runTest {
        servidor.enqueue(MockResponse().setBody("<!doctype html><html><body>bloqueado</body></html>"))

        esperarFalha<FalhaXtream.RespostaInvalida> { api.autenticar() }
    }

    @Test
    fun `redirecionamento para outro host vira RespostaInvalida`() = runTest {
        // Como o get.php do provedor real, que responde 302 para www.google.com (ver design,
        // §3.1). followRedirects(false) tem que impedir o cliente de seguir esse redirect.
        servidor.enqueue(
            MockResponse().setResponseCode(302).addHeader("Location", "http://www.google.com/"),
        )

        esperarFalha<FalhaXtream.RespostaInvalida> { api.autenticar() }
    }

    @Test
    fun `lerLista entrega em lotes sem passar do tamanho`() = runTest {
        val itens = (1..1234).joinToString(",") { """{"stream_id":$it,"name":"Canal $it"}""" }
        servidor.enqueue(MockResponse().setBody("[$itens]"))

        val lotes = mutableListOf<Int>()
        val total = api.lerLista("get_live_streams", 500) { lote -> lotes += lote.size }

        assertEquals(listOf(500, 500, 234), lotes)
        assertEquals(1234, total)
        assertTrue(servidor.takeRequest().path!!.endsWith("&action=get_live_streams"))
    }

    @Test
    fun `abrirXmltv entrega o corpo da resposta para quem le`() = runTest {
        servidor.enqueue(MockResponse().setBody("<tv><programme/></tv>"))

        val lido = api.abrirXmltv { it.readBytes().decodeToString() }

        assertEquals("<tv><programme/></tv>", lido)
    }

    @Test
    fun `IOException ao ler o corpo do xmltv vira ServidorIndisponivel`() = runTest {
        // Simula a conexão caindo no meio do download do XMLTV (8,2 MB no provedor real,
        // propenso a instabilidade de rede) — quem lê é o parser, não executar(), então essa
        // IOException não passava pelo try/catch de executar().
        servidor.enqueue(MockResponse().setBody("<tv><programme/></tv>"))

        esperarFalha<FalhaXtream.ServidorIndisponivel> {
            api.abrirXmltv { throw IOException("conexão perdida") }
        }
    }

    @Test
    fun `XmlPullParserException ao ler o corpo do xmltv vira RespostaInvalida`() = runTest {
        servidor.enqueue(MockResponse().setBody("<tv><programme/></tv>"))

        esperarFalha<FalhaXtream.RespostaInvalida> {
            api.abrirXmltv { throw XmlPullParserException("tag inválida") }
        }
    }
}
