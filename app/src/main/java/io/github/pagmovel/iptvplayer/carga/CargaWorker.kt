package io.github.pagmovel.iptvplayer.carga
import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.pagmovel.iptvplayer.AppIptv
import io.github.pagmovel.iptvplayer.conta.ContasRepositorio
import io.github.pagmovel.iptvplayer.conta.CredenciaisXtream
import io.github.pagmovel.iptvplayer.dados.BancoApp
import io.github.pagmovel.iptvplayer.xtream.FalhaXtream
import io.github.pagmovel.iptvplayer.xtream.XtreamApi
import io.github.pagmovel.iptvplayer.xtream.mensagemUsuario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val CHAVE_CONTA_ID = "contaId"
private const val CHAVE_ETAPA = "etapa"
private const val CHAVE_MENSAGEM = "mensagem"

/**
 * Carrega o catálogo de uma conta em segundo plano (ver [CargaCatalogo]), via WorkManager.
 *
 * O construtor de 5 parâmetros recebe as dependências direto — é o que os testes usam, com
 * `TestListenableWorkerBuilder` + uma `WorkerFactory` de teste (ver `CargaWorkerTest`), sem
 * precisar do [AppIptv] real (que tem um [io.github.pagmovel.iptvplayer.conta.CofreKeystore]
 * que não funciona sob Robolectric). O construtor de 2 parâmetros é o que o WorkManager chama
 * por reflexão em produção (`WorkerFactory.createWorkerWithDefaultFallback`, que procura
 * exatamente um construtor `(Context, WorkerParameters)`), e resolve as dependências reais a
 * partir do [AppIptv] — sem framework de DI, como o resto do app.
 */
class CargaWorker(
    appContext: Context,
    params: WorkerParameters,
    private val banco: BancoApp,
    private val contasRepositorio: ContasRepositorio,
    private val criarApi: (CredenciaisXtream) -> XtreamApi,
) : CoroutineWorker(appContext, params) {

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        (appContext.applicationContext as AppIptv).banco,
        (appContext.applicationContext as AppIptv).contasRepositorio,
        (appContext.applicationContext as AppIptv).criarApi,
    )

    override suspend fun doWork(): Result {
        val contaId = inputData.getLong(CHAVE_CONTA_ID, -1L)
        if (contaId == -1L) return Result.failure(falha("Conta inválida."))
        val conta = banco.contaDao().listar().first().firstOrNull { it.id == contaId }
            ?: return Result.failure(falha("Conta não encontrada."))
        return try {
            val api = criarApi(contasRepositorio.credenciais(conta))
            CargaCatalogo(api, banco, contaId).executar { etapa ->
                // ponytail: setProgress é suspend e o callback de executar() não é; como o
                // worker já roda numa thread de fundo (nunca a principal — ver doc de
                // CoroutineWorker, usa Dispatchers.Default por padrão), bloquear essa thread
                // de fundo brevemente com runBlocking é aceitável. Trocar o parâmetro
                // `progresso` de CargaCatalogo.executar para suspend se isso emperrar a carga.
                runBlocking { setProgress(workDataOf(CHAVE_ETAPA to etapa.name)) }
            }
            banco.contaDao().marcarCarga(contaId, System.currentTimeMillis())
            Result.success()
        } catch (e: FalhaXtream) {
            Result.failure(falha(e.mensagemUsuario()))
        }
    }

    private fun falha(mensagem: String) = workDataOf(CHAVE_MENSAGEM to mensagem)

    companion object {
        /** Nome do trabalho único do WorkManager para a conta — no máximo uma carga por vez por conta. */
        fun nomeTrabalho(contaId: Long) = "carga-$contaId"

        /** Enfileira a carga da conta [contaId], substituindo nenhuma carga já em andamento dela. */
        fun enfileirar(workManager: WorkManager, contaId: Long) {
            val pedido = OneTimeWorkRequestBuilder<CargaWorker>()
                .setInputData(workDataOf(CHAVE_CONTA_ID to contaId))
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .build()
            workManager.enqueueUniqueWork(nomeTrabalho(contaId), ExistingWorkPolicy.KEEP, pedido)
        }
    }
}
