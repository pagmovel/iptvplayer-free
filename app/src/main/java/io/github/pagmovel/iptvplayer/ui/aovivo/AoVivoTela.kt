package io.github.pagmovel.iptvplayer.ui.aovivo

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.PlayerView
import io.github.pagmovel.iptvplayer.aovivo.EventoCascata
import io.github.pagmovel.iptvplayer.dados.Programa
import kotlinx.coroutines.delay

private enum class AbaAoVivo { CANAIS, PROGRAMACAO }

/** Quanto tempo sem toque na tela ate o OSD se esconder sozinho -- decisao minha (T9), o
 * design so diz "alguns segundos sem interacao" sem fixar um numero. */
private const val OSD_AUTO_ESCONDER_MS = 5_000L

/**
 * Tela de TV ao vivo (briefs T7/T8, design Sec. "TV ao vivo"): player em cima, abas Canais/
 * Programacao embaixo.
 *
 * Global Constraint (segundo plano): o video para em ON_STOP e reabre do zero em ON_START --
 * e transmissao ao vivo, nao ha posicao pra retomar. ON_DISPOSE libera o ExoPlayer de
 * verdade, pra nao vazar ao sair da tela.
 */
@Composable
fun AoVivoTela(
    viewModel: AoVivoViewModel,
    onVoltar: () -> Unit,
    onGerenciarContas: () -> Unit,
    onAbrirConfiguracoes: () -> Unit,
) {
    var carregando by remember { mutableStateOf(true) }
    var aba by remember { mutableStateOf(AbaAoVivo.CANAIS) }
    var grade by remember { mutableStateOf(false) }
    var osdVisivel by remember { mutableStateOf(true) }
    var toques by remember { mutableStateOf(0) }
    fun aoTocarNaTela() {
        osdVisivel = true
        toques++
    }
    // Reinicia a cada toque: LaunchedEffect cancela a corrotina anterior e comeca outra
    // sempre que a chave (toques) muda -- mesmo efeito de um timer que reseta do zero.
    LaunchedEffect(toques) {
        delay(OSD_AUTO_ESCONDER_MS)
        osdVisivel = false
    }

    LaunchedEffect(viewModel) {
        viewModel.carregar()
        carregando = false
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_STOP -> viewModel.pausar()
                Lifecycle.Event.ON_START -> viewModel.retomar()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.liberar()
        }
    }

    if (carregando) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val canalAtual = viewModel.canalAtual
    val programaDoCanalAtual by produceState<Programa?>(initialValue = null, canalAtual) {
        value = canalAtual?.let { viewModel.programaAtual(it) }
    }

    // Passo 1 (T10): celular deitado = so video + OSD em tela cheia. LocalConfiguration.current
    // ja recompoe sozinho quando a orientacao muda (sem precisar de DisposableEffect/listener
    // proprio); como o manifest ja declara configChanges="orientation|screenSize|screenLayout"
    // (T6), a Activity nao recria e o PlayerView/ExoPlayer nao sao recriados por isso.
    val emPaisagem = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Revisao final (Importante 7): sem canal selecionado (abrir a tela ja deitado, ou voltar
    // de Configuracoes deitado) nao pode cair em tela cheia sem saida -- usa a tela dividida,
    // com a lista de canais visivel, mesmo em paisagem.
    val telaCheia = emPaisagem && canalAtual != null

    // Bug real pos-T10 (validacao manual do dono): T10 so escondia a NOSSA UI (Voltar/abas) em
    // telaCheia -- as barras do SISTEMA (status + navegacao do Android) continuavam por cima do
    // video. setDecorFitsSystemWindows(false) fica condicionado ao mesmo telaCheia (em vez de
    // uma chamada unica no onCreate da Activity) pra nao afetar o layout do resto do app (Resumo/
    // Contas/Configuracoes), que nunca entra em telaCheia. onDispose cobre tanto "voltou para
    // retrato" (DisposableEffect refaz com a chave nova) quanto "saiu da tela de TV ao vivo
    // direto de paisagem" (composable sai de composicao) -- os dois precisam restaurar as barras.
    val view = LocalView.current
    DisposableEffect(telaCheia) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        if (telaCheia) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (!telaCheia) {
                TextButton(onClick = onVoltar) { Text("Voltar") }
            }

            Box(
                (if (telaCheia) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(220.dp))
                    .clickable(onClick = { aoTocarNaTela() }),
            ) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            player = viewModel.exoPlayer
                            // Critico 1: sem isso o Android apaga a tela pelo timeout do
                            // sistema e dispara ON_STOP (que pausa o player) so por falta de
                            // toque, mesmo com o usuario assistindo.
                            keepScreenOn = true
                            // Bug real pos-T11 (validacao manual do dono, print real): sem
                            // isso o controle NATIVO do Media3 (retroceder/avancar 10s, play/
                            // pause, item anterior/proximo de playlist, barra de progresso)
                            // aparecia ao tocar na tela em vez do nosso Osd (que tem "< Canal"/
                            // "Canal >", qualidade, etc). useController=true e o padrao do
                            // Media3; so o nosso Osd (acionado pelo .clickable do Box pai) pode
                            // controlar a interacao aqui.
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Bug real pos-T11: spinner de buffering sobreposto ao video -- cobre tanto o
                // carregamento normal quanto a troca de qualidade da cascata (os dois emitem
                // STATE_BUFFERING no PlayerAoVivo). Nao aparece sobre o aviso de FalhaTerminal
                // porque PlayerAoVivo zera bufferizando nesse ramo (ver PlayerAoVivo.abrir).
                val bufferizando by viewModel.bufferizando.collectAsState()
                if (bufferizando) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                Osd(
                    visivel = osdVisivel,
                    canal = canalAtual,
                    programaAtual = programaDoCanalAtual,
                    eventoAtual = viewModel.eventoAtual,
                    aoTocarNaTela = { aoTocarNaTela() },
                    onVoltar = onVoltar,
                    onEscolherQualidade = { viewModel.trocarQualidadeManual(it) },
                    onCanalAnterior = { viewModel.canalAnterior() },
                    onCanalProximo = { viewModel.canalProximo() },
                    onAbrirProgramacao = { grade = true },
                    onGerenciarContas = onGerenciarContas,
                    onAbrirConfiguracoes = onAbrirConfiguracoes,
                )

                // Global Constraint: so mostra erro (e o botao manual) quando a qualidade mais
                // baixa tambem falhou -- nunca tenta de novo sozinho sem esse toque. Desenhado
                // DEPOIS do Osd (Importante 5): o Osd ocupa a tela toda e e clicavel, entao
                // desenhado antes ele intercepta o toque antes de chegar no botao "Tentar de
                // novo" por baixo.
                if (viewModel.eventoAtual == EventoCascata.FalhaTerminal) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Nao foi possivel tocar o canal, nem na qualidade mais baixa.",
                                color = MaterialTheme.colorScheme.onError,
                            )
                            Button(onClick = { viewModel.tentarNovamente() }) { Text("Tentar de novo") }
                        }
                    }
                }
            }

            // Paisagem (Passo 1, T10): sem Voltar/abas por fora -- o Osd ja tem seu proprio
            // "voltar", e a grade de programacao (overlay abaixo) nao depende de aba nenhuma.
            if (!telaCheia) {
                TabRow(selectedTabIndex = aba.ordinal) {
                    Tab(selected = aba == AbaAoVivo.CANAIS, onClick = { aba = AbaAoVivo.CANAIS }, text = { Text("Canais") })
                    Tab(
                        selected = aba == AbaAoVivo.PROGRAMACAO,
                        onClick = { aba = AbaAoVivo.PROGRAMACAO },
                        text = { Text("Programacao") },
                    )
                }

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (aba) {
                        AbaAoVivo.CANAIS -> CanaisAba(viewModel, onSelecionarCanal = { viewModel.selecionarCanal(it) })
                        AbaAoVivo.PROGRAMACAO -> ProgramacaoAba(viewModel)
                    }
                }
            }
        }

        // Grade de programacao como overlay em tela cheia (atalho do OSD, brief T9) -- reusa
        // a ProgramacaoAba (T8) por cima de tudo, sem sair da tela de TV ao vivo. Independe de
        // orientacao (Passo 1, T10): e overlay de tela cheia nos dois casos.
        if (grade) {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                TextButton(onClick = { grade = false }) { Text("Fechar") }
                ProgramacaoAba(viewModel)
            }
        }
    }
}
