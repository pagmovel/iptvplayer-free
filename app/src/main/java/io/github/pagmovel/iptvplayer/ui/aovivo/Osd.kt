package io.github.pagmovel.iptvplayer.ui.aovivo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.pagmovel.iptvplayer.aovivo.CanalAgrupado
import io.github.pagmovel.iptvplayer.aovivo.EventoCascata
import io.github.pagmovel.iptvplayer.aovivo.Qualidade
import io.github.pagmovel.iptvplayer.dados.Programa
import kotlinx.coroutines.delay

/** Quanto tempo o selo de "Mudou para X" fica na tela antes de sumir sozinho -- decisao
 * minha (T9), o design nao fixa um valor (so fixa o timer de 8s da cascata, que e outra
 * coisa: o tempo de buffer ate degradar, nao o tempo que o aviso fica visivel). */
private const val SELO_DURACAO_MS = 3_000L

/**
 * OSD (overlay) do player de TV ao vivo -- brief T9, design Sec. "TV ao vivo" > "Controles do
 * player (OSD)". [AoVivoTela] controla [visivel] (toque na tela mostra e reinicia o timer de
 * auto-esconder via [aoTocarNaTela]); aqui dentro, cada acao tambem chama [aoTocarNaTela] ao
 * ser tocada, pra nao sumir no meio de uma interacao.
 *
 * O selo de troca de qualidade ("Mudou para X") e independente de [visivel]: a troca
 * automatica pode acontecer sem nenhum toque na tela (ex. usuario so assistindo), e o aviso
 * precisa aparecer mesmo com o resto do OSD escondido.
 */
@Composable
fun Osd(
    visivel: Boolean,
    canal: CanalAgrupado?,
    programaAtual: Programa?,
    eventoAtual: EventoCascata?,
    aoTocarNaTela: () -> Unit,
    onVoltar: () -> Unit,
    onEscolherQualidade: (Qualidade) -> Unit,
    onCanalAnterior: () -> Unit,
    onCanalProximo: () -> Unit,
    onAbrirProgramacao: () -> Unit,
    onGerenciarContas: () -> Unit,
    onAbrirConfiguracoes: () -> Unit,
) {
    var seloEvento by remember { mutableStateOf<EventoCascata.Trocou?>(null) }
    LaunchedEffect(eventoAtual) {
        if (eventoAtual is EventoCascata.Trocou) {
            seloEvento = eventoAtual
            delay(SELO_DURACAO_MS)
            seloEvento = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (visivel) {
            // Qualidade tocando agora, pra destacar no seletor -- deriva de eventoAtual em vez
            // de precisar de outro campo no ViewModel (Tocando/Trocou sempre carregam a
            // variante/qualidade corrente; FalhaTerminal e null nao tem uma pra destacar).
            val qualidadeAtual = when (eventoAtual) {
                is EventoCascata.Tocando -> eventoAtual.variante.qualidade
                is EventoCascata.Trocou -> eventoAtual.para
                else -> null
            }
            fun comToque(acao: () -> Unit): () -> Unit = { aoTocarNaTela(); acao() }

            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(onClick = aoTocarNaTela)
                    .padding(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = comToque(onVoltar)) { Text("Voltar", color = Color.White) }
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(canal?.nomeBase ?: "", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Text(
                            programaAtual?.titulo ?: "Sem programacao",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(Modifier.padding(top = 4.dp)) {
                    canal?.variantes?.forEach { variante ->
                        TextButton(onClick = comToque { onEscolherQualidade(variante.qualidade) }) {
                            Text(
                                variante.qualidade.rotulo,
                                color = if (variante.qualidade == qualidadeAtual) Color.Yellow else Color.White,
                            )
                        }
                    }
                }
                Row(Modifier.padding(top = 4.dp)) {
                    TextButton(onClick = comToque(onCanalAnterior)) { Text("< Canal", color = Color.White) }
                    TextButton(onClick = comToque(onAbrirProgramacao)) { Text("Programacao", color = Color.White) }
                    TextButton(onClick = comToque(onCanalProximo)) { Text("Canal >", color = Color.White) }
                }
                Row(Modifier.padding(top = 4.dp)) {
                    TextButton(onClick = comToque(onGerenciarContas)) { Text("Gerenciar contas", color = Color.White) }
                    TextButton(onClick = comToque(onAbrirConfiguracoes)) { Text("Configuracoes", color = Color.White) }
                }
            }
        }

        val evento = seloEvento
        if (evento != null) {
            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.TopCenter) {
                Text(
                    "Mudou para ${evento.para.rotulo}",
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}
