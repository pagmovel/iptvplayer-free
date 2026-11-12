package io.github.pagmovel.iptvplayer.ui.aovivo

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.PlayerView
import io.github.pagmovel.iptvplayer.aovivo.EventoCascata

private enum class AbaAoVivo { CANAIS, PROGRAMACAO }

/**
 * Tela de TV ao vivo (brief T7, design Sec. "TV ao vivo"): player em cima, abas Canais/
 * Programacao embaixo. A aba Programacao (T8) ainda nao existe -- fica com um placeholder.
 *
 * Global Constraint (segundo plano): o video para em ON_STOP e reabre do zero em ON_START --
 * e transmissao ao vivo, nao ha posicao pra retomar. ON_DISPOSE libera o ExoPlayer de
 * verdade, pra nao vazar ao sair da tela.
 */
@Composable
fun AoVivoTela(viewModel: AoVivoViewModel, onVoltar: () -> Unit) {
    var carregando by remember { mutableStateOf(true) }
    var aba by remember { mutableStateOf(AbaAoVivo.CANAIS) }

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

    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onVoltar) { Text("Voltar") }

        Box(Modifier.fillMaxWidth().height(220.dp)) {
            AndroidView(
                factory = { context -> PlayerView(context).apply { player = viewModel.exoPlayer } },
                modifier = Modifier.fillMaxSize(),
            )

            // Global Constraint: so mostra erro (e o botao manual) quando a qualidade mais
            // baixa tambem falhou -- nunca tenta de novo sozinho sem esse toque.
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
                AbaAoVivo.PROGRAMACAO -> Text("Programacao chega na T8.", Modifier.padding(16.dp))
            }
        }
    }
}
