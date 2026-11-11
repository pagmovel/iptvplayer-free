package io.github.pagmovel.iptvplayer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.pagmovel.iptvplayer.AppIptv

/**
 * Hospeda a navegação manual entre Contas e Resumo (ver [Navegacao]), com o tema escuro do
 * design (ver [TemaIptv]).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TemaIptv {
                Navegacao(applicationContext as AppIptv)
            }
        }
    }
}
