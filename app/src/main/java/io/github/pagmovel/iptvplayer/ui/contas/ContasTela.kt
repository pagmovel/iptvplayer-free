@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.pagmovel.iptvplayer.ui.contas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import io.github.pagmovel.iptvplayer.conta.CredenciaisXtream
import io.github.pagmovel.iptvplayer.conta.lerLinkXtream
import io.github.pagmovel.iptvplayer.dados.Conta
import kotlinx.coroutines.launch

/**
 * Tela de contas (brief T11): lista, adicionar (com "colar link do provedor"), ativar,
 * editar servidor e apagar. Tela provisória — plano 2/3 pode trazer navegação própria.
 */
@Composable
fun ContasTela(viewModel: ContasViewModel, onVoltar: () -> Unit) {
    val contas by viewModel.contas.collectAsState(initial = emptyList())
    var mostrarFormulario by remember { mutableStateOf(false) }
    var contaEmEdicao by remember { mutableStateOf<Conta?>(null) }
    var contaParaApagar by remember { mutableStateOf<Conta?>(null) }
    val escopo = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contas") },
                navigationIcon = { TextButton(onClick = onVoltar) { Text("Voltar") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { contaEmEdicao = null; mostrarFormulario = true }) {
                Text("+")
            }
        },
    ) { preenchimento ->
        LazyColumn(modifier = Modifier.padding(preenchimento).fillMaxSize()) {
            items(contas, key = { it.id }) { conta ->
                LinhaConta(
                    conta = conta,
                    onAtivar = { escopo.launch { viewModel.ativar(conta.id) } },
                    onEditar = { contaEmEdicao = conta; mostrarFormulario = true },
                    onApagar = { contaParaApagar = conta },
                )
            }
        }
    }

    if (mostrarFormulario) {
        FormularioConta(
            viewModel = viewModel,
            contaEmEdicao = contaEmEdicao,
            onFechar = { mostrarFormulario = false },
        )
    }

    contaParaApagar?.let { conta ->
        AlertDialog(
            onDismissRequest = { contaParaApagar = null },
            title = { Text("Apagar conta") },
            text = { Text("Apagar \"${conta.nome}\" e todo o catálogo dela?") },
            confirmButton = {
                TextButton(onClick = {
                    escopo.launch { viewModel.apagar(conta.id) }
                    contaParaApagar = null
                }) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { contaParaApagar = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun LinhaConta(conta: Conta, onAtivar: () -> Unit, onEditar: () -> Unit, onApagar: () -> Unit) {
    ListItem(
        headlineContent = { Text(conta.nome + if (conta.ativa) " (ativa)" else "") },
        supportingContent = { Text(conta.servidor) },
        trailingContent = {
            Column {
                if (!conta.ativa) TextButton(onClick = onAtivar) { Text("Ativar") }
                TextButton(onClick = onEditar) { Text("Editar") }
                TextButton(onClick = onApagar) { Text("Apagar") }
            }
        },
    )
}

@Composable
private fun FormularioConta(viewModel: ContasViewModel, contaEmEdicao: Conta?, onFechar: () -> Unit) {
    var nome by remember { mutableStateOf(contaEmEdicao?.nome ?: "") }
    var servidor by remember { mutableStateOf(contaEmEdicao?.servidor ?: "") }
    var usuario by remember { mutableStateOf(contaEmEdicao?.usuario ?: "") }
    var senha by remember { mutableStateOf("") }
    var avisoLink by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val escopo = rememberCoroutineScope()
    val ehEdicao = contaEmEdicao != null

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(if (ehEdicao) "Editar servidor" else "Adicionar conta") },
        text = {
            Column {
                if (!ehEdicao) {
                    OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") })
                    TextButton(onClick = {
                        val texto = clipboard.getText()?.text.orEmpty()
                        val cred = lerLinkXtream(texto)
                        if (cred == null) {
                            avisoLink = "Link não reconhecido"
                        } else {
                            avisoLink = null
                            servidor = cred.servidor
                            usuario = cred.usuario
                            senha = cred.senha
                        }
                    }) { Text("Colar link do provedor") }
                    avisoLink?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                OutlinedTextField(value = servidor, onValueChange = { servidor = it }, label = { Text("Servidor") })
                if (!ehEdicao) {
                    OutlinedTextField(value = usuario, onValueChange = { usuario = it }, label = { Text("Usuário") })
                    OutlinedTextField(value = senha, onValueChange = { senha = it }, label = { Text("Senha") })
                }
                viewModel.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (viewModel.carregando) CircularProgressIndicator()
            }
        },
        confirmButton = {
            TextButton(onClick = {
                escopo.launch {
                    val ok = if (contaEmEdicao != null) {
                        viewModel.editarServidor(contaEmEdicao, servidor)
                        true
                    } else {
                        viewModel.salvar(nome, CredenciaisXtream(servidor, usuario, senha))
                    }
                    if (ok) onFechar()
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}
