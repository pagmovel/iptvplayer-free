package io.github.pagmovel.iptvplayer.ui.resumo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import io.github.pagmovel.iptvplayer.carga.EtapaCarga
import io.github.pagmovel.iptvplayer.dados.Conta
import io.github.pagmovel.iptvplayer.dados.Contagem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela de resumo (brief T11) — tela inicial provisória, que o Plano 3 troca pela vitrine.
 * Sem conta ativa, [onPrecisaDeConta] leva direto para o formulário de cadastro.
 */
@Composable
fun ResumoTela(
    viewModel: ResumoViewModel,
    onPrecisaDeConta: () -> Unit,
    onGerenciarContas: () -> Unit,
    onAssistirAoVivo: () -> Unit,
) {
    var conta by remember { mutableStateOf<Conta?>(null) }
    var contagem by remember { mutableStateOf(Contagem(0, 0, 0, 0)) }
    var carregando by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val ativa = viewModel.contaAtiva()
        if (ativa == null) {
            onPrecisaDeConta()
            return@LaunchedEffect
        }
        conta = ativa
        contagem = viewModel.contagem(ativa.id)
        carregando = false
        if (viewModel.precisaAtualizar(ativa)) viewModel.atualizarAgora(ativa.id)
    }

    val contaAtual = conta
    if (contaAtual == null) {
        if (carregando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        return
    }

    val info by viewModel.progresso(contaAtual.id).collectAsState(initial = null)

    LaunchedEffect(info?.state) {
        if (info?.state == WorkInfo.State.SUCCEEDED) {
            contagem = viewModel.contagem(contaAtual.id)
            conta = viewModel.contaAtiva()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(contaAtual.nome, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text("Canais: ${contagem.canais}")
        Text("Filmes: ${contagem.filmes}")
        Text("Séries: ${contagem.series}")
        Text("Programas: ${contagem.programas}")
        Spacer(Modifier.height(16.dp))
        EstadoCarga(info)
        Spacer(Modifier.height(8.dp))
        Text("Última carga: " + formatarData(contaAtual.ultimaCargaEm))
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.atualizarAgora(contaAtual.id) }) { Text("Atualizar agora") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onGerenciarContas) { Text("Gerenciar contas") }
        TextButton(onClick = onAssistirAoVivo) { Text("Assistir ao vivo") }
    }
}

@Composable
private fun EstadoCarga(info: WorkInfo?) {
    val texto = when (info?.state) {
        WorkInfo.State.RUNNING -> "Atualizando: " + nomeEtapa(info.progress.getString("etapa"))
        WorkInfo.State.ENQUEUED -> "Aguardando conexão com a internet."
        WorkInfo.State.FAILED -> info.outputData.getString("mensagem") ?: "Falha ao atualizar."
        else -> null
    }
    texto?.let { Text(it) }
}

private fun nomeEtapa(nome: String?): String = when (nome) {
    EtapaCarga.AUTENTICANDO.name -> "autenticando"
    EtapaCarga.CATEGORIAS.name -> "categorias"
    EtapaCarga.CANAIS.name -> "canais"
    EtapaCarga.FILMES.name -> "filmes"
    EtapaCarga.SERIES.name -> "séries"
    EtapaCarga.PROGRAMACAO.name -> "programação"
    EtapaCarga.CONCLUIDA.name -> "concluída"
    else -> "…"
}

private fun formatarData(epochMs: Long?): String =
    if (epochMs == null) {
        "nunca"
    } else {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(epochMs))
    }
