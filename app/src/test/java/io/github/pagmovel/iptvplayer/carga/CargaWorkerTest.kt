package io.github.pagmovel.iptvplayer.carga
import android.content.Context
import androidx.room.Room
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import io.github.pagmovel.iptvplayer.conta.Cofre
import io.github.pagmovel.iptvplayer.conta.ContasRepositorio
import io.github.pagmovel.iptvplayer.conta.CredenciaisXtream
import io.github.pagmovel.iptvplayer.dados.BancoApp
import io.github.pagmovel.iptvplayer.xtream.XtreamApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Cofre falso em memória — mesmo motivo de [io.github.pagmovel.iptvplayer.conta.ContasRepositorioTest]:
 * Robolectric não emula o `AndroidKeyStore`.
 */
private class CofreFalso : Cofre {
    override fun cifrar(texto: String): ByteArray = texto.toByteArray(Charsets.UTF_8)
    override fun decifrar(dados: ByteArray): String = dados.toString(Charsets.UTF_8)
}

@RunWith(RobolectricTestRunner::class)
class CargaWorkerTest {
    private lateinit var servidor: MockWebServer
    private lateinit var banco: BancoApp
    private lateinit var repo: ContasRepositorio
    private var contaId: Long = -1

    @Before
    fun montar() = runTest {
        servidor = MockWebServer()
        servidor.start()
        banco = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), BancoApp::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ContasRepositorio(banco, CofreFalso()) { cred -> XtreamApi(OkHttpClient(), cred) }
        servidor.enqueue(auth())
        val cred = CredenciaisXtream(servidor.url("/").toString().removeSuffix("/"), "u", "p")
        contaId = repo.testarESalvar("Conta A", cred).getOrThrow().id
    }

    @After
    fun desmontar() {
        banco.close()
        servidor.shutdown()
    }

    private fun auth() = MockResponse().setBody(
        """{"user_info":{"auth":1,"status":"Active","exp_date":"1800000000","max_connections":"1"}}""",
    )

    private fun authInvalida() = MockResponse().setBody("""{"user_info":{"auth":0}}""")

    private fun lista() = MockResponse().setBody("[]")

    /** Constrói o worker com as dependências de teste direto, via uma WorkerFactory própria —
     * bypassa o [io.github.pagmovel.iptvplayer.AppIptv] real (ver comentário de [CargaWorker]). */
    private fun criarWorker(): CargaWorker {
        val fabrica = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker = CargaWorker(appContext, workerParameters, banco, repo) { cred ->
                XtreamApi(OkHttpClient(), cred)
            }
        }
        return TestListenableWorkerBuilder<CargaWorker>(
            RuntimeEnvironment.getApplication(),
            inputData = workDataOf("contaId" to contaId),
        ).setWorkerFactory(fabrica).build()
    }

    @Test
    fun `worker grava o catalogo e marca a carga`() = runTest {
        servidor.enqueue(auth()) // autenticar (CargaCatalogo)
        servidor.enqueue(lista()) // get_live_categories
        servidor.enqueue(lista()) // get_vod_categories
        servidor.enqueue(lista()) // get_series_categories
        servidor.enqueue(lista()) // get_live_streams
        servidor.enqueue(lista()) // get_vod_streams
        servidor.enqueue(lista()) // get_series
        servidor.enqueue(MockResponse().setBody("<tv></tv>")) // xmltv.php

        val resultado = criarWorker().doWork()

        assertTrue(resultado is ListenableWorker.Result.Success)
        val conta = banco.contaDao().listar().first().single { it.id == contaId }
        assertNotNull(conta.ultimaCargaEm)
    }

    @Test
    fun `worker traduz falha em mensagem`() = runTest {
        servidor.enqueue(authInvalida()) // autenticar (CargaCatalogo) falha: auth = 0

        val resultado = criarWorker().doWork()

        assertTrue(resultado is ListenableWorker.Result.Failure)
        val mensagem = (resultado as ListenableWorker.Result.Failure).outputData.getString("mensagem")
        assertEquals("Usuário ou senha incorretos", mensagem)
    }
}
