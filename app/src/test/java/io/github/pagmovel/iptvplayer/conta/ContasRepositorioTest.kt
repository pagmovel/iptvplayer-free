package io.github.pagmovel.iptvplayer.conta
import androidx.room.Room
import io.github.pagmovel.iptvplayer.dados.BancoApp
import io.github.pagmovel.iptvplayer.dados.Canal
import io.github.pagmovel.iptvplayer.dados.Conta
import io.github.pagmovel.iptvplayer.xtream.XtreamApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
/**
 * Cofre falso em memória: XOR reversível por byte, só para o teste comprovar que a senha
 * salva não é texto puro (Robolectric não emula o `AndroidKeyStore`, ver [CofreKeystore] —
 * é por isso que [Cofre] é uma interface). Não é prefixo/sufixo porque isso deixaria a senha
 * original como substring do resultado, o que o teste 1 tem que reprovar de verdade.
 */
private class CofreFalso : Cofre {
    override fun cifrar(texto: String): ByteArray =
        texto.toByteArray(Charsets.UTF_8).map { (it.toInt() xor 0xFF).toByte() }.toByteArray()
    override fun decifrar(dados: ByteArray): String =
        dados.map { (it.toInt() xor 0xFF).toByte() }.toByteArray().toString(Charsets.UTF_8)
}
@RunWith(RobolectricTestRunner::class)
class ContasRepositorioTest {
    private lateinit var servidor: MockWebServer
    private lateinit var banco: BancoApp
    private lateinit var repo: ContasRepositorio
    @Before
    fun montar() {
        servidor = MockWebServer()
        servidor.start()
        banco = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), BancoApp::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ContasRepositorio(banco, CofreFalso()) { cred -> XtreamApi(OkHttpClient(), cred) }
    }
    @After
    fun desmontar() {
        banco.close()
        servidor.shutdown()
    }
    private fun credenciais(usuario: String = "u", senha: String = "s3nh4") =
        CredenciaisXtream(servidor.url("/").toString().removeSuffix("/"), usuario, senha)
    private fun auth() = MockResponse().setBody(
        """{"user_info":{"auth":1,"status":"Active","exp_date":"1800000000","max_connections":"1"}}""",
    )
    private fun authInvalida() = MockResponse().setBody("""{"user_info":{"auth":0}}""")
    private fun contaSolta(ultimaCargaEm: Long?) = Conta(
        id = 1,
        nome = "x",
        servidor = "x",
        usuario = "x",
        senhaCifrada = ByteArray(0),
        ativa = false,
        ultimaCargaEm = ultimaCargaEm,
    )
    @Test
    fun `senha nunca e gravada em texto puro`() = runTest {
        servidor.enqueue(auth())
        val conta = repo.testarESalvar("Conta A", credenciais(senha = "s3nh4")).getOrThrow()
        // ISO-8859-1 mapeia cada byte a um char sem perdas (ao contrário de UTF-8, que pode
        // substituir byte inválido) — é o jeito certo de procurar a senha como substring dos
        // bytes crus, não do texto decodificado.
        val bytesComoTexto = String(conta.senhaCifrada, Charsets.ISO_8859_1)
        assertFalse(bytesComoTexto.contains("s3nh4"))
    }
    @Test
    fun `credencial invalida nao salva conta`() = runTest {
        servidor.enqueue(authInvalida())
        val resultado = repo.testarESalvar("Conta A", credenciais())
        assertTrue(resultado.isFailure)
        assertTrue(banco.contaDao().listar().first().isEmpty())
    }
    @Test
    fun `primeira conta fica ativa e ativar troca`() = runTest {
        servidor.enqueue(auth())
        val a = repo.testarESalvar("Conta A", credenciais(usuario = "a")).getOrThrow()
        servidor.enqueue(auth())
        val b = repo.testarESalvar("Conta B", credenciais(usuario = "b")).getOrThrow()
        val depoisDeCriar = banco.contaDao().listar().first()
        assertEquals(a.id, depoisDeCriar.single { it.ativa }.id)
        repo.ativar(b.id)
        val depoisDeAtivar = banco.contaDao().listar().first()
        assertEquals(b.id, depoisDeAtivar.single { it.ativa }.id)
    }
    @Test
    fun `apagar conta apaga o catalogo dela`() = runTest {
        servidor.enqueue(auth())
        val conta = repo.testarESalvar("Conta A", credenciais()).getOrThrow()
        banco.catalogoDao().inserirCanais(
            listOf(
                Canal(
                    contaId = conta.id,
                    streamId = 1,
                    nome = "Canal 1",
                    categoriaId = null,
                    icone = null,
                    epgId = null,
                    numero = null,
                ),
            ),
        )
        repo.apagar(conta.id)
        assertEquals(0, banco.catalogoDao().contagem(conta.id).canais)
        assertTrue(banco.contaDao().listar().first().isEmpty())
    }
    @Test
    fun `precisa atualizar depois de 12 horas`() = runTest {
        assertTrue(repo.precisaAtualizar(contaSolta(ultimaCargaEm = null), agoraMs = 1_000_000L))
        val onzeH59 = (11 * 60 + 59) * 60 * 1000L
        assertFalse(repo.precisaAtualizar(contaSolta(ultimaCargaEm = 0L), agoraMs = onzeH59))
        val dozeH01 = (12 * 60 + 1) * 60 * 1000L
        assertTrue(repo.precisaAtualizar(contaSolta(ultimaCargaEm = 0L), agoraMs = dozeH01))
    }
}
