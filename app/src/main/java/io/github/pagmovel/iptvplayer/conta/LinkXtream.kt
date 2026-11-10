package io.github.pagmovel.iptvplayer.conta

import java.net.URL
import java.net.URLDecoder

data class CredenciaisXtream(
    val servidor: String,
    val usuario: String,
    val senha: String
)

fun lerLinkXtream(texto: String): CredenciaisXtream? {
    if (texto.isBlank()) return null

    // Encontrar primeira URL no padrão https?://\S+
    val urlRegex = Regex("https?://\\S+")
    val urlMatch = urlRegex.find(texto) ?: return null
    val url = urlMatch.value

    return try {
        val uri = URL(url)
        val query = uri.query ?: return null

        // Parsear os parâmetros
        val params = query.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            val key = parts[0]
            val value = if (parts.size > 1) parts[1] else ""
            key to value
        }

        // Extrair e decodificar usuario e senha
        val usuario = params["username"]?.let { URLDecoder.decode(it, "UTF-8") } ?: return null
        val senha = params["password"]?.let { URLDecoder.decode(it, "UTF-8") } ?: return null

        // Construir servidor: scheme + host + porta (se houver)
        val servidor = buildString {
            append(uri.protocol)
            append("://")
            append(uri.host)
            if (uri.port != -1) {
                append(":")
                append(uri.port)
            }
        }

        CredenciaisXtream(servidor, usuario, senha)
    } catch (e: Exception) {
        null
    }
}
