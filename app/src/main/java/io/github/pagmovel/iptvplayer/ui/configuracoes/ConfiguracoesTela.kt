package io.github.pagmovel.iptvplayer.ui.configuracoes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Tela de Configurações (brief T10, design Sec. "TV ao vivo" > "Configurações") — único campo
 * por enquanto: tempo de buffer/erro antes da troca automática de qualidade (T4/T5). Acessível
 * pelo atalho do OSD (T9, [io.github.pagmovel.iptvplayer.ui.aovivo.Osd]) e pela tela de Resumo
 * (onde "Gerenciar contas" já estava, Plano 1).
 */
@Composable
fun ConfiguracoesTela(viewModel: ConfiguracoesViewModel, onVoltar: () -> Unit) {
    var tempoBufferTexto by remember { mutableStateOf("") }
    var salvo by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        tempoBufferTexto = viewModel.tempoBufferSegundos().toString()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onVoltar) { Text("Voltar") }
        Spacer(Modifier.height(8.dp))
        Text("Configurações", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = tempoBufferTexto,
            onValueChange = { tempoBufferTexto = it; salvo = false },
            label = { Text("Tempo de buffer antes de trocar de qualidade (segundos)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {
            val segundos = tempoBufferTexto.toIntOrNull()
            if (segundos != null && segundos > 0) {
                viewModel.definirTempoBufferSegundos(segundos)
                salvo = true
            }
        }) { Text("Salvar") }
        if (salvo) Text("Salvo.")
    }
}
