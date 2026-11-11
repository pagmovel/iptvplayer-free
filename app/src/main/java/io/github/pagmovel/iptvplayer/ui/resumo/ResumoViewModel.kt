package io.github.pagmovel.iptvplayer.ui.resumo
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.pagmovel.iptvplayer.carga.CargaWorker
import io.github.pagmovel.iptvplayer.conta.ContasRepositorio
import io.github.pagmovel.iptvplayer.dados.BancoApp
import io.github.pagmovel.iptvplayer.dados.Conta
import io.github.pagmovel.iptvplayer.dados.Contagem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Estado e ações da tela de resumo — mesma observação de
 * [io.github.pagmovel.iptvplayer.ui.contas.ContasViewModel] sobre não ser um
 * `androidx.lifecycle.ViewModel`.
 */
class ResumoViewModel(
    private val banco: BancoApp,
    private val repositorio: ContasRepositorio,
    private val workManager: WorkManager,
) {
    suspend fun contaAtiva(): Conta? = banco.contaDao().ativa()

    suspend fun contagem(contaId: Long): Contagem = banco.catalogoDao().contagem(contaId)

    suspend fun precisaAtualizar(conta: Conta): Boolean =
        repositorio.precisaAtualizar(conta, System.currentTimeMillis())

    fun atualizarAgora(contaId: Long) = CargaWorker.enfileirar(workManager, contaId)

    /** Último (e único — trabalho único por conta) [WorkInfo] da carga dessa conta, se houver. */
    fun progresso(contaId: Long): Flow<WorkInfo?> =
        workManager.getWorkInfosForUniqueWorkFlow(CargaWorker.nomeTrabalho(contaId)).map { it.firstOrNull() }
}
