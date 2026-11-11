package io.github.pagmovel.iptvplayer.carga

import androidx.room.withTransaction
import io.github.pagmovel.iptvplayer.dados.BancoApp
import io.github.pagmovel.iptvplayer.dados.CatalogoDao
import io.github.pagmovel.iptvplayer.dados.Contagem
import io.github.pagmovel.iptvplayer.dados.Programa
import io.github.pagmovel.iptvplayer.dados.TipoConteudo
import io.github.pagmovel.iptvplayer.programacao.janelaProgramacao
import io.github.pagmovel.iptvplayer.programacao.lerXmltv
import io.github.pagmovel.iptvplayer.xtream.XtreamApi
import io.github.pagmovel.iptvplayer.xtream.paraCanal
import io.github.pagmovel.iptvplayer.xtream.paraCategoria
import io.github.pagmovel.iptvplayer.xtream.paraFilme
import io.github.pagmovel.iptvplayer.xtream.paraSerie
import java.time.ZonedDateTime

/**
 * Etapa da carga, na ordem em que [CargaCatalogo.executar] as percorre. Serve só para
 * informar progresso a quem chama (ex.: uma tela de carga) — nenhuma etapa é pulada nem
 * repetida numa carga que roda até o fim.
 */
enum class EtapaCarga { AUTENTICANDO, CATEGORIAS, CANAIS, FILMES, SERIES, PROGRAMACAO, CONCLUIDA }

/**
 * Orquestra a carga completa do catálogo de uma conta Xtream (ver design, §3.1): autentica,
 * depois troca categorias/canais/filmes/séries **numa única transação** e, por fim, troca a
 * programação **noutra transação separada**.
 *
 * As duas trocas são transações independentes de propósito, não uma só: falha na programação
 * não desfaz o catálogo, porque um canal sem programação é aceito pelo design. Já falha em
 * qualquer etapa do catálogo (categorias/canais/filmes/séries) desfaz a troca inteira — ou o
 * catálogo novo entra completo, ou o catálogo antigo continua valendo. Falha na autenticação
 * acontece antes de qualquer transação, então não apaga nada.
 */
class CargaCatalogo(
    private val api: XtreamApi,
    private val banco: BancoApp,
    private val contaId: Long,
    private val agora: () -> ZonedDateTime = { ZonedDateTime.now() },
) {
    private val dao: CatalogoDao get() = banco.catalogoDao()

    suspend fun executar(progresso: (EtapaCarga) -> Unit = {}): Contagem {
        progresso(EtapaCarga.AUTENTICANDO)
        api.autenticar()

        banco.withTransaction {
            dao.apagarCatalogo(contaId)

            progresso(EtapaCarga.CATEGORIAS)
            carregarCategorias(TipoConteudo.AO_VIVO, "get_live_categories")
            carregarCategorias(TipoConteudo.FILME, "get_vod_categories")
            carregarCategorias(TipoConteudo.SERIE, "get_series_categories")

            progresso(EtapaCarga.CANAIS)
            api.lerLista("get_live_streams") { lote ->
                dao.inserirCanais(lote.mapNotNull { it.paraCanal(contaId) })
            }

            progresso(EtapaCarga.FILMES)
            api.lerLista("get_vod_streams") { lote ->
                dao.inserirFilmes(lote.mapNotNull { it.paraFilme(contaId) })
            }

            progresso(EtapaCarga.SERIES)
            api.lerLista("get_series") { lote ->
                dao.inserirSeries(lote.mapNotNull { it.paraSerie(contaId) })
            }
        }

        progresso(EtapaCarga.PROGRAMACAO)
        banco.withTransaction {
            dao.apagarProgramacao(contaId)
            val janela = janelaProgramacao(agora())
            // lerXmltv (T8) é bloqueante, não suspend — o callback só acumula os lotes aqui;
            // quem grava é o loop abaixo, de volta no escopo suspend desta transação. Evita
            // depender de como o Room liga uma chamada suspend feita por dentro de uma ponte
            // bloqueante (ex.: runBlocking) à conexão da transação em andamento.
            val lotes = mutableListOf<List<Programa>>()
            api.abrirXmltv { entrada -> lerXmltv(entrada, contaId, janela) { lote -> lotes += lote } }
            for (lote in lotes) dao.inserirProgramas(lote)
        }

        progresso(EtapaCarga.CONCLUIDA)
        return dao.contagem(contaId)
    }

    private suspend fun carregarCategorias(tipo: TipoConteudo, acao: String) {
        api.lerLista(acao) { lote ->
            dao.inserirCategorias(lote.mapNotNull { it.paraCategoria(contaId, tipo) })
        }
    }
}
