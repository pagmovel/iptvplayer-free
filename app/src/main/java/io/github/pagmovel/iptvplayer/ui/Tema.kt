package io.github.pagmovel.iptvplayer.ui
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/**
 * Tema escuro do design aprovado (esboços em `.superpowers/brainstorm/`): fundo quase preto,
 * destaque azul-ciano, e nenhum texto do app abaixo de 14sp.
 */
private val Fundo = Color(0xFF0F1115)
private val Destaque = Color(0xFF4FC3F7)

private val cores = darkColorScheme(
    primary = Destaque,
    secondary = Destaque,
    background = Fundo,
    surface = Fundo,
)

// Só os estilos que, por padrão, ficam abaixo de 14sp no Material 3 (bodySmall 12sp,
// labelMedium 12sp, labelSmall 11sp) — o resto já atende o mínimo sem precisar mudar.
private val base = Typography()
private val tipografia = base.copy(
    bodySmall = base.bodySmall.copy(fontSize = 14.sp),
    labelMedium = base.labelMedium.copy(fontSize = 14.sp),
    labelSmall = base.labelSmall.copy(fontSize = 14.sp),
)

@Composable
fun TemaIptv(conteudo: @Composable () -> Unit) {
    MaterialTheme(colorScheme = cores, typography = tipografia) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            conteudo()
        }
    }
}
