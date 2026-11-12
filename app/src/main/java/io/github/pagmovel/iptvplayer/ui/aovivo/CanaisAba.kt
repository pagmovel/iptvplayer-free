package io.github.pagmovel.iptvplayer.ui.aovivo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pagmovel.iptvplayer.aovivo.CanalAgrupado
import io.github.pagmovel.iptvplayer.dados.Programa
import kotlinx.coroutines.launch

/**
 * Aba "Canais" da tela de TV ao vivo (brief T7, design Sec. "TV ao vivo"): chips de categoria
 * (Todos / Favoritos / categorias reais, selecao unica) e a lista de canais agrupados por
 * qualidade -- cada linha com estrela de favorito e o programa que esta no ar agora (ou
 * "Sem programacao").
 */
@Composable
fun CanaisAba(viewModel: AoVivoViewModel, onSelecionarCanal: (CanalAgrupado) -> Unit) {
    val categorias by viewModel.categorias.collectAsState(initial = emptyList())
    val favoritos by viewModel.favoritos.collectAsState(initial = emptyList())
    val canais by viewModel.canaisFiltrados.collectAsState(initial = emptyList())
    val chavesFavoritas = remember(favoritos) { favoritos.map { it.nomeBase to it.categoriaId }.toSet() }
    val escopo = rememberCoroutineScope()

    var categoriaSelecionada by remember { mutableStateOf<Long?>(null) }
    var somenteFavoritos by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = categoriaSelecionada == null && !somenteFavoritos,
                    onClick = {
                        categoriaSelecionada = null
                        somenteFavoritos = false
                        viewModel.filtrarPorCategoria(null)
                        viewModel.filtrarFavoritos(false)
                    },
                    label = { Text("Todos") },
                )
            }
            item {
                FilterChip(
                    selected = somenteFavoritos,
                    onClick = {
                        categoriaSelecionada = null
                        somenteFavoritos = true
                        viewModel.filtrarPorCategoria(null)
                        viewModel.filtrarFavoritos(true)
                    },
                    label = { Text("Favoritos") },
                )
            }
            items(categorias, key = { it.categoriaId }) { categoria ->
                FilterChip(
                    selected = !somenteFavoritos && categoriaSelecionada == categoria.categoriaId,
                    onClick = {
                        categoriaSelecionada = categoria.categoriaId
                        somenteFavoritos = false
                        viewModel.filtrarPorCategoria(categoria.categoriaId)
                        viewModel.filtrarFavoritos(false)
                    },
                    label = { Text(categoria.nome) },
                )
            }
        }

        if (canais.isEmpty()) {
            Text("Nenhum canal nesse filtro.", Modifier.padding(16.dp))
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(canais, key = { it.nomeBase + "|" + it.categoriaId }) { canal ->
                val favorito = (canal.nomeBase to canal.categoriaId) in chavesFavoritas
                LinhaCanal(
                    canal = canal,
                    favorito = favorito,
                    carregarPrograma = { viewModel.programaAtual(canal) },
                    onClick = { onSelecionarCanal(canal) },
                    onAlternarFavorito = {
                        escopo.launch {
                            if (favorito) viewModel.desfavoritar(canal) else viewModel.favoritar(canal)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LinhaCanal(
    canal: CanalAgrupado,
    favorito: Boolean,
    carregarPrograma: suspend () -> Programa?,
    onClick: () -> Unit,
    onAlternarFavorito: () -> Unit,
) {
    val programa by produceState<Programa?>(initialValue = null, canal) { value = carregarPrograma() }

    ListItem(
        headlineContent = { Text(canal.nomeBase) },
        supportingContent = { Text(programa?.titulo ?: "Sem programacao") },
        leadingContent = { IconeOuNome(canal.nomeBase) },
        trailingContent = {
            TextButton(onClick = onAlternarFavorito) { Text(if (favorito) "★" else "☆") }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

/**
 * ponytail: sem lib de imagem (Coil/Glide) no projeto ainda -- mostra sempre o nome sobre
 * fundo neutro, mesma regra do design Sec. 4 pra item sem capa. Trocar por um
 * AsyncImage(canal.icone) quando uma lib de imagem entrar no projeto (T7 nao precisou).
 */
@Composable
private fun IconeOuNome(nome: String) {
    Box(
        Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(nome.take(2).uppercase(), style = MaterialTheme.typography.labelSmall)
    }
}
