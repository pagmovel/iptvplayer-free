package io.github.pagmovel.iptvplayer.ui.aovivo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pagmovel.iptvplayer.dados.Programa
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Aba "Programacao" da tela de TV ao vivo (brief T8, design Sec. "TV ao vivo"): seletor
 * Ontem/Hoje/Amanha e a programacao do dia inteiro do canal atualmente selecionado --
 * "Sem programacao" tanto por canal sem EPG quanto por janela sem nenhum programa.
 */
@Composable
fun ProgramacaoAba(viewModel: AoVivoViewModel) {
    var dia by remember { mutableStateOf(DiaPrograma.HOJE) }
    // Revisao final (Importante 6): o canal atual entra na chave -- so `dia` deixava a grade
    // presa no canal anterior ao trocar de canal (ex. "Canal >" do OSD) com a aba aberta.
    val programas by produceState(initialValue = emptyList<Programa>(), dia, viewModel.canalAtual) {
        value = viewModel.programacaoDoDia(dia)
    }

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = dia == DiaPrograma.ONTEM,
                    onClick = { dia = DiaPrograma.ONTEM },
                    label = { Text("Ontem") },
                )
            }
            item {
                FilterChip(
                    selected = dia == DiaPrograma.HOJE,
                    onClick = { dia = DiaPrograma.HOJE },
                    label = { Text("Hoje") },
                )
            }
            item {
                FilterChip(
                    selected = dia == DiaPrograma.AMANHA,
                    onClick = { dia = DiaPrograma.AMANHA },
                    label = { Text("Amanha") },
                )
            }
        }

        if (programas.isEmpty()) {
            Text("Sem programacao.", Modifier.padding(16.dp))
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(programas, key = { it.inicio }) { programa ->
                ListItem(
                    headlineContent = { Text(programa.titulo) },
                    supportingContent = { Text(formatarHorario(programa.inicio, programa.fim)) },
                )
            }
        }
    }
}

private fun formatarHorario(inicioMs: Long, fimMs: Long): String {
    val formato = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
    return "${formato.format(Date(inicioMs))} - ${formato.format(Date(fimMs))}"
}
