package io.github.pagmovel.iptvplayer.xtream

import io.github.pagmovel.iptvplayer.conta.CredenciaisXtream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeToSequence
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Cliente da API Xtream Codes (`player_api.php`) do provedor (formato real confirmado no
 * design, §3.1). Nunca segue redirecionamento para outro host: o provedor redireciona os
 * links de lista M3U para `www.google.com`, e isso tem que virar
 * [FalhaXtream.RespostaInvalida] em vez de seguir o redirect.
 */
class XtreamApi(
    private val cliente: OkHttpClient,
    private val cred: CredenciaisXtream,
) {
    data class InfoConta(val ativa: Boolean, val expiraEm: Long?, val maxConexoes: Int?)

    suspend fun autenticar(): InfoConta = withContext(Dispatchers.IO) {
        val corpo = executar(montarRequisicao(acao = null), timeoutSegundos = 20)
        val usuario = lerJson(corpo)["user_info"] as? JsonObject
            ?: throw FalhaXtream.RespostaInvalida("resposta sem \"user_info\"")

        if (usuario.int("auth") != 1) throw FalhaXtream.CredenciaisInvalidas()
        val status = usuario.texto("status") ?: ""
        if (status != "Active") throw FalhaXtream.ContaInativa(status)

        InfoConta(
            ativa = true,
            expiraEm = usuario.long("exp_date"),
            maxConexoes = usuario.int("max_connections"),
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun lerLista(
        acao: String,
        tamanhoLote: Int = 500,
        aoLote: suspend (List<JsonObject>) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        val corpo = executar(montarRequisicao(acao), timeoutSegundos = 120)
        try {
            val iterador = Json.decodeToSequence<JsonObject>(
                corpo.byteStream(),
                DecodeSequenceMode.ARRAY_WRAPPED,
            ).iterator()

            var total = 0
            while (iterador.hasNext()) {
                val lote = ArrayList<JsonObject>(tamanhoLote)
                while (lote.size < tamanhoLote && iterador.hasNext()) {
                    lote.add(iterador.next())
                }
                aoLote(lote)
                total += lote.size
            }
            total
        } catch (e: SerializationException) {
            throw FalhaXtream.RespostaInvalida(e.message ?: "JSON inválido")
        } catch (e: IOException) {
            throw FalhaXtream.ServidorIndisponivel(e)
        } finally {
            corpo.close()
        }
    }

    suspend fun <T> abrirXmltv(ler: suspend (InputStream) -> T): T = withContext(Dispatchers.IO) {
        val corpo = executar(montarRequisicao(acao = null, caminho = "xmltv.php"), timeoutSegundos = 120)
        try {
            corpo.byteStream().use { ler(it) }
        } catch (e: XmlPullParserException) {
            throw FalhaXtream.RespostaInvalida(e.message ?: "XMLTV inválido")
        } catch (e: IOException) {
            throw FalhaXtream.ServidorIndisponivel(e)
        }
    }

    fun urlAoVivo(streamId: Long): String =
        "${cred.servidor}/live/${cred.usuario}/${cred.senha}/$streamId.ts"

    fun urlFilme(streamId: Long, extensao: String): String =
        "${cred.servidor}/movie/${cred.usuario}/${cred.senha}/$streamId.$extensao"

    fun urlEpisodio(id: Long, extensao: String): String =
        "${cred.servidor}/series/${cred.usuario}/${cred.senha}/$id.$extensao"

    private fun montarRequisicao(acao: String?, caminho: String = "player_api.php"): Request {
        val url = buildString {
            append(cred.servidor)
            append("/")
            append(caminho)
            append("?username=")
            append(cred.usuario)
            append("&password=")
            append(cred.senha)
            if (acao != null) {
                append("&action=")
                append(acao)
            }
        }
        return Request.Builder().url(url).build()
    }

    /** Bloqueia a thread atual — só é chamado de dentro de [Dispatchers.IO]. */
    private fun executar(requisicao: Request, timeoutSegundos: Long): ResponseBody {
        val chamada = cliente.newBuilder()
            .followRedirects(false)
            .callTimeout(timeoutSegundos, TimeUnit.SECONDS)
            .build()
            .newCall(requisicao)

        val resposta = try {
            chamada.execute()
        } catch (e: IOException) {
            throw FalhaXtream.ServidorIndisponivel(e)
        }

        if (resposta.code in 300..399) {
            resposta.close()
            throw FalhaXtream.RespostaInvalida("redirecionamento para outro host (HTTP ${resposta.code})")
        }
        if (!resposta.isSuccessful) {
            resposta.close()
            throw FalhaXtream.ServidorIndisponivel(IOException("HTTP ${resposta.code}"))
        }
        // Response.body não é nulo a partir do OkHttp 5 (corpo vazio em vez de null).
        return resposta.body
    }

    private fun lerJson(corpo: ResponseBody): JsonObject = corpo.use {
        try {
            Json.parseToJsonElement(it.string()) as JsonObject
        } catch (e: Exception) {
            throw FalhaXtream.RespostaInvalida(e.message ?: "JSON inválido")
        }
    }
}
