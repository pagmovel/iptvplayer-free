package io.github.pagmovel.iptvplayer.ui
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.work.WorkManager
import io.github.pagmovel.iptvplayer.AppIptv
import io.github.pagmovel.iptvplayer.ui.aovivo.AoVivoTela
import io.github.pagmovel.iptvplayer.ui.aovivo.AoVivoViewModel
import io.github.pagmovel.iptvplayer.ui.contas.ContasTela
import io.github.pagmovel.iptvplayer.ui.contas.ContasViewModel
import io.github.pagmovel.iptvplayer.ui.resumo.ResumoTela
import io.github.pagmovel.iptvplayer.ui.resumo.ResumoViewModel

/**
 * Navegação manual entre as telas do app (Resumo, Contas e Ao vivo) — sem
 * `androidx.navigation:navigation-compose`, que o catálogo nem declara: uma troca simples de
 * estado resolve, sem precisar de rotas/back stack de uma biblioteca de navegação (ver YAGNI
 * do brief T11).
 */
private enum class Tela { RESUMO, CONTAS, AO_VIVO }

@Composable
fun Navegacao(app: AppIptv) {
    var tela by remember { mutableStateOf(Tela.RESUMO) }
    // Distingue "sem conta, forçar o cadastro" (brief T11: abre direto o formulário) de
    // "usuário escolheu gerenciar contas" (não abre o formulário sozinho).
    var forcarFormulario by remember { mutableStateOf(false) }
    val workManager = remember { WorkManager.getInstance(app) }
    when (tela) {
        Tela.RESUMO -> ResumoTela(
            viewModel = remember { ResumoViewModel(app.banco, app.contasRepositorio, workManager) },
            onPrecisaDeConta = { forcarFormulario = true; tela = Tela.CONTAS },
            onGerenciarContas = { forcarFormulario = false; tela = Tela.CONTAS },
            onAssistirAoVivo = { tela = Tela.AO_VIVO },
        )
        Tela.CONTAS -> ContasTela(
            viewModel = remember { ContasViewModel(app.contasRepositorio) },
            onVoltar = { tela = Tela.RESUMO },
            abrirFormularioInicialmente = forcarFormulario,
        )
        Tela.AO_VIVO -> AoVivoTela(
            viewModel = remember { AoVivoViewModel(app) },
            onVoltar = { tela = Tela.RESUMO },
            onGerenciarContas = { forcarFormulario = false; tela = Tela.CONTAS },
            // TODO(T10): navegar pra Tela.CONFIGURACOES -- a tela ainda nao existe (escopo
            // da T10, que vem depois desta T9).
            onAbrirConfiguracoes = {},
        )
    }
}
