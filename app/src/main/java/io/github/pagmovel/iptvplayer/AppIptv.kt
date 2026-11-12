package io.github.pagmovel.iptvplayer
import android.app.Application
import androidx.room.Room
import io.github.pagmovel.iptvplayer.conta.Cofre
import io.github.pagmovel.iptvplayer.conta.CofreKeystore
import io.github.pagmovel.iptvplayer.conta.ContasRepositorio
import io.github.pagmovel.iptvplayer.conta.CredenciaisXtream
import io.github.pagmovel.iptvplayer.dados.BancoApp
import io.github.pagmovel.iptvplayer.xtream.XtreamApi
import okhttp3.OkHttpClient

/**
 * Monta à mão as dependências do app — banco, cofre de senhas e repositório de contas — sem
 * framework de DI (mesma decisão do resto do app). [io.github.pagmovel.iptvplayer.carga.CargaWorker]
 * lê essas dependências de volta daqui, pelo construtor de 2 parâmetros que o WorkManager usa
 * por reflexão (ver o construtor secundário dele); não precisa de `Configuration.Provider`
 * nem de `WorkerFactory` customizada no `Configuration` do app.
 */
class AppIptv : Application() {
    val banco: BancoApp by lazy {
        Room.databaseBuilder(this, BancoApp::class.java, "iptv.db")
            .addMigrations(BancoApp.MIGRACAO_1_2)
            .build()
    }
    val cofre: Cofre by lazy { CofreKeystore() }
    val criarApi: (CredenciaisXtream) -> XtreamApi = { cred -> XtreamApi(OkHttpClient(), cred) }
    val contasRepositorio: ContasRepositorio by lazy { ContasRepositorio(banco, cofre, criarApi) }
}
