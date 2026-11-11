package io.github.pagmovel.iptvplayer.conta
import io.github.pagmovel.iptvplayer.dados.BancoApp
import io.github.pagmovel.iptvplayer.dados.Conta
import io.github.pagmovel.iptvplayer.xtream.FalhaXtream
import io.github.pagmovel.iptvplayer.xtream.XtreamApi
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
/** Conta não fica sem atualizar o catálogo por mais de 12h (ver design, §3.1). */
private const val INTERVALO_CARGA_MS = 12 * 60 * 60 * 1000L
/**
 * Cadastro e consulta das contas Xtream do app: testa a credencial antes de salvar, cifra a
 * senha com [cofre] e decide qual conta está ativa. `criarApi` existe para o teste poder
 * trocar [XtreamApi] por uma instância apontando para um `MockWebServer`, sem o repositório
 * precisar saber disso.
 */
class ContasRepositorio(
    private val banco: BancoApp,
    private val cofre: Cofre,
    private val criarApi: (CredenciaisXtream) -> XtreamApi,
) {
    /** Só salva se [CredenciaisXtream] autenticar de verdade. A primeira conta cadastrada fica ativa. */
    suspend fun testarESalvar(nome: String, cred: CredenciaisXtream): Result<Conta> =
        try {
            criarApi(cred).autenticar()
            val primeira = banco.contaDao().listar().first().isEmpty()
            val conta = Conta(
                nome = nome,
                servidor = cred.servidor,
                usuario = cred.usuario,
                senhaCifrada = cofre.cifrar(cred.senha),
                ativa = primeira,
                ultimaCargaEm = null,
            )
            val id = banco.contaDao().inserir(conta)
            Result.success(conta.copy(id = id))
        } catch (e: FalhaXtream) {
            Result.failure(e)
        }
    /**
     * Deixa exatamente a conta [id] ativa, desativando as demais.
     *
     * ponytail: lê e regrava todas as contas (uma a uma); aceitável porque o app guarda
     * poucas contas (cadastro manual, uma por vez). Se isso crescer, trocar por duas queries
     * `UPDATE` diretas no [ContaDao] (zera todas, ativa uma).
     */
    suspend fun ativar(id: Long) = banco.withTransaction {
        banco.contaDao().listar().first().forEach { conta ->
            banco.contaDao().atualizar(conta.copy(ativa = conta.id == id))
        }
    }
    /** Apaga a conta e todo o catálogo/programação dela. */
    suspend fun apagar(id: Long) = banco.withTransaction {
        banco.catalogoDao().apagarCatalogo(id)
        banco.catalogoDao().apagarProgramacao(id)
        banco.contaDao().apagar(id)
    }
    /** Decifra a senha salva para montar as credenciais usadas numa nova carga/autenticação. */
    suspend fun credenciais(conta: Conta): CredenciaisXtream =
        CredenciaisXtream(conta.servidor, conta.usuario, cofre.decifrar(conta.senhaCifrada))
    /** Verdadeiro se a conta nunca carregou ou se a última carga passou de 12h. */
    suspend fun precisaAtualizar(conta: Conta, agoraMs: Long): Boolean {
        val ultima = conta.ultimaCargaEm ?: return true
        return agoraMs - ultima > INTERVALO_CARGA_MS
    }
    /** Todas as contas salvas (a tela de contas se atualiza sozinha, ver [ContaDao.listar]). */
    fun listar(): Flow<List<Conta>> = banco.contaDao().listar()
    /**
     * Troca só o servidor da conta (T11: o provedor dá vários endereços para a mesma conta,
     * ver design §2). Não testa a credencial de novo contra o servidor novo antes de salvar —
     * se o endereço estiver errado, a próxima carga reporta isso como uma falha normal.
     */
    suspend fun editarServidor(conta: Conta, servidor: String) {
        banco.contaDao().atualizar(conta.copy(servidor = servidor))
    }
}
