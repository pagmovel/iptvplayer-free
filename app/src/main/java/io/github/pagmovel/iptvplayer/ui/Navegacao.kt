package io.github.pagmovel.iptvplayer.ui
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.work.WorkManager
import io.github.pagmovel.iptvplayer.AppIptv
import io.github.pagmovel.iptvplayer.ui.contas.ContasTela
import io.github.pagmovel.iptvplayer.ui.contas.ContasViewModel
import io.github.pagmovel.iptvplayer.ui.resumo.ResumoTela
import io.github.pagmovel.iptvplayer.ui.resumo.ResumoViewModel

/**
 * Navegação manual entre as duas telas do app (Contas e Resumo) — sem
 * `androidx.navigation:navigation-compose`, que o catálogo nem declara: o app só tem duas
 * telas e uma troca simples de estado resolve, sem precisar de rotas/back stack de uma
 * biblioteca de navegação (ver YAGNI do brief T11).
 */
private enum class Tela { RESUMO, CONTAS }

@Composable
fun Navegacao(app: AppIptv) {
    var tela by remember { mutableStateOf(Tela.RESUMO) }
    val workManager = remember { WorkManager.getInstance(app) }
    when (tela) {
        Tela.RESUMO -> ResumoTela(
            viewModel = remember { ResumoViewModel(app.banco, app.contasRepositorio, workManager) },
            onPrecisaDeConta = { tela = Tela.CONTAS },
            onGerenciarContas = { tela = Tela.CONTAS },
        )
        Tela.CONTAS -> ContasTela(
            viewModel = remember { ContasViewModel(app.contasRepositorio) },
            onVoltar = { tela = Tela.RESUMO },
        )
    }
}
