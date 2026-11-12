package io.github.pagmovel.iptvplayer.ui.aovivo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.exoplayer.ExoPlayer
import io.github.pagmovel.iptvplayer.AppIptv
import io.github.pagmovel.iptvplayer.aovivo.CanalAgrupado
import io.github.pagmovel.iptvplayer.aovivo.CascataQualidade
import io.github.pagmovel.iptvplayer.aovivo.EventoCascata
import io.github.pagmovel.iptvplayer.aovivo.PlayerAoVivo
import io.github.pagmovel.iptvplayer.aovivo.agruparPorQualidade
import io.github.pagmovel.iptvplayer.dados.CanalFavorito
import io.github.pagmovel.iptvplayer.dados.Categoria
import io.github.pagmovel.iptvplayer.dados.Programa
import io.github.pagmovel.iptvplayer.dados.TipoConteudo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Programas de ate 24h atras ate agora -- janela ampla o suficiente pra conter o que esta
 * no ar, sem depender de um horario fixo de inicio de dia. */
private const val JANELA_PROGRAMACAO_MS = 24 * 60 * 60 * 1000L

/** Entre os programas retornados por [CatalogoDao.programacao], o que esta no ar agora --
 * comeca antes (ou agora) e ainda nao terminou. */
fun programaNoInstante(programas: List<Programa>, agora: Long): Programa? =
    programas.lastOrNull { it.inicio <= agora && it.fim > agora }

/** Filtro aplicado a lista de canais agrupados: por categoria (null = Todos) e/ou so favoritos. */
fun filtrarCanais(
    canais: List<CanalAgrupado>,
    favoritos: List<CanalFavorito>,
    categoriaId: Long?,
    somenteFavoritos: Boolean,
): List<CanalAgrupado> {
    val chavesFavoritas = favoritos.map { it.nomeBase to it.categoriaId }.toSet()
    return canais.filter { canal ->
        (categoriaId == null || canal.categoriaId == categoriaId) &&
            (!somenteFavoritos || (canal.nomeBase to canal.categoriaId) in chavesFavoritas)
    }
}

/**
 * Estado e acoes da tela de TV ao vivo -- mesma observacao de
 * [io.github.pagmovel.iptvplayer.ui.contas.ContasViewModel] sobre nao ser um
 * `androidx.lifecycle.ViewModel`.
 *
 * [carregar] resolve a conta ativa e monta o [PlayerAoVivo] -- so precisa ser chamado uma vez
 * (a conta ativa nao muda durante a sessao da tela); so depois dele os `Flow`s de categorias/
 * favoritos/canais ficam disponiveis (ver [AoVivoTela]).
 */
class AoVivoViewModel(private val app: AppIptv) {
    private val banco get() = app.banco

    private var contaId: Long = 0L
    private lateinit var playerAoVivo: PlayerAoVivo

    private val filtroCategoria = MutableStateFlow<Long?>(null)
    private val filtroSomenteFavoritos = MutableStateFlow(false)

    // Mesmo dispatcher do escopo interno de PlayerAoVivo (Dispatchers.Main.immediate): a
    // inscricao em cascata.eventos precisa estar ativa ANTES de selecionarCanal chamar
    // playerAoVivo.abrir(), que chama cascata.iniciar() -- eventos e um SharedFlow sem
    // replay, perderia o primeiro Tocando senao (mesmo achado real documentado em
    // PlayerAoVivo.kt).
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var cascataAtual: CascataQualidade? = null
    private var canalAtual: CanalAgrupado? = null
    private var jobEventos: Job? = null

    var eventoAtual by mutableStateOf<EventoCascata?>(null)
        private set

    val exoPlayer: ExoPlayer get() = playerAoVivo.exoPlayer

    lateinit var categorias: Flow<List<Categoria>>
        private set
    lateinit var favoritos: Flow<List<CanalFavorito>>
        private set
    lateinit var canaisFiltrados: Flow<List<CanalAgrupado>>
        private set

    suspend fun carregar() {
        val conta = requireNotNull(banco.contaDao().ativa()) { "sem conta ativa" }
        contaId = conta.id
        val cred = app.contasRepositorio.credenciais(conta)
        val api = app.criarApi(cred)
        playerAoVivo = PlayerAoVivo(app) { streamId -> api.urlAoVivo(streamId) }

        categorias = banco.catalogoDao().categorias(contaId, TipoConteudo.AO_VIVO)
        favoritos = banco.catalogoDao().favoritos(contaId)
        val canaisAgrupados = banco.catalogoDao().canais(contaId).map { agruparPorQualidade(it) }
        canaisFiltrados = combine(
            canaisAgrupados,
            favoritos,
            filtroCategoria,
            filtroSomenteFavoritos,
        ) { canais, favs, categoriaId, somenteFav -> filtrarCanais(canais, favs, categoriaId, somenteFav) }
    }

    /** Programa no ar agora no canal, ou null se nao tem EPG ou a janela nao tem nada no ar. */
    suspend fun programaAtual(canal: CanalAgrupado): Programa? {
        val epgId = canal.epgId ?: return null
        val agora = System.currentTimeMillis()
        val programas = banco.catalogoDao().programacao(contaId, epgId, agora - JANELA_PROGRAMACAO_MS, agora + 1)
        return programaNoInstante(programas, agora)
    }

    fun filtrarPorCategoria(categoriaId: Long?) {
        filtroCategoria.value = categoriaId
    }

    fun filtrarFavoritos(somente: Boolean) {
        filtroSomenteFavoritos.value = somente
    }

    suspend fun favoritar(canal: CanalAgrupado) {
        banco.catalogoDao().favoritar(
            CanalFavorito(contaId, canal.nomeBase, canal.categoriaId, System.currentTimeMillis()),
        )
    }

    suspend fun desfavoritar(canal: CanalAgrupado) {
        banco.catalogoDao().desfavoritar(contaId, canal.nomeBase, canal.categoriaId)
    }

    /** Cria uma cascata nova pro canal e abre no player, cancelando a cascata anterior.
     * Le [ConfiguracoesRepositorio.tempoBufferMs] a cada chamada -- uma troca feita em
     * Configuracoes so vale a partir do proximo canal aberto. */
    fun selecionarCanal(canal: CanalAgrupado) {
        if (!::playerAoVivo.isInitialized) return
        jobEventos?.cancel()
        canalAtual = canal
        eventoAtual = null

        val cascata = CascataQualidade(canal, app.configuracoes.tempoBufferMs(), escopo)
        cascataAtual = cascata
        jobEventos = escopo.launch {
            cascata.eventos.collect { evento -> eventoAtual = evento }
        }
        playerAoVivo.abrir(canal, cascata)
    }

    fun tentarNovamente() {
        cascataAtual?.tentarNovamente()
    }

    /** ON_STOP (Global Constraint): pausa sem liberar o player. */
    fun pausar() {
        if (::playerAoVivo.isInitialized) playerAoVivo.exoPlayer.pause()
    }

    /** ON_START: reabre o canal atual do zero -- e transmissao ao vivo, nao ha posicao pra retomar. */
    fun retomar() {
        canalAtual?.let { selecionarCanal(it) }
    }

    /** ON_DISPOSE: libera o ExoPlayer de verdade e cancela a coleta de eventos da tela. */
    fun liberar() {
        jobEventos?.cancel()
        if (::playerAoVivo.isInitialized) playerAoVivo.liberar()
        escopo.cancel()
    }
}
