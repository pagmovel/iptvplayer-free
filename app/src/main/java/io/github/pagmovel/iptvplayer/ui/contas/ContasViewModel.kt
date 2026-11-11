package io.github.pagmovel.iptvplayer.ui.contas
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.pagmovel.iptvplayer.conta.ContasRepositorio
import io.github.pagmovel.iptvplayer.conta.CredenciaisXtream
import io.github.pagmovel.iptvplayer.dados.Conta
import io.github.pagmovel.iptvplayer.xtream.FalhaXtream
import io.github.pagmovel.iptvplayer.xtream.mensagemUsuario
import kotlinx.coroutines.flow.Flow

/**
 * Estado e ações da tela de contas.
 *
 * ponytail: não é um `androidx.lifecycle.ViewModel` — é só um guardador de estado comum
 * (`remember` na tela, ver [io.github.pagmovel.iptvplayer.ui.Navegacao]), suficiente para
 * esta tela. Perde o formulário aberto numa rotação de tela; trocar por
 * `androidx.lifecycle.ViewModel` (já disponível via activity-compose, sem dependência nova)
 * se isso incomodar de verdade no uso real.
 */
class ContasViewModel(private val repositorio: ContasRepositorio) {
    val contas: Flow<List<Conta>> = repositorio.listar()

    var carregando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
        private set

    /** Testa a credencial e salva; `erro` fica com a mensagem traduzida se falhar. */
    suspend fun salvar(nome: String, cred: CredenciaisXtream): Boolean {
        carregando = true
        erro = null
        val resultado = repositorio.testarESalvar(nome, cred)
        carregando = false
        (resultado.exceptionOrNull() as? FalhaXtream)?.let { erro = it.mensagemUsuario() }
        return resultado.isSuccess
    }

    suspend fun ativar(id: Long) = repositorio.ativar(id)

    suspend fun apagar(id: Long) = repositorio.apagar(id)

    suspend fun editarServidor(conta: Conta, servidor: String) = repositorio.editarServidor(conta, servidor)

    fun limparErro() {
        erro = null
    }
}
