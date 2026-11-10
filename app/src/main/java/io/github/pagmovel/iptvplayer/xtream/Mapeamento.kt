package io.github.pagmovel.iptvplayer.xtream

import io.github.pagmovel.iptvplayer.dados.Canal
import io.github.pagmovel.iptvplayer.dados.Categoria
import io.github.pagmovel.iptvplayer.dados.Filme
import io.github.pagmovel.iptvplayer.dados.Serie
import io.github.pagmovel.iptvplayer.dados.TipoConteudo
import kotlinx.serialization.json.JsonObject

/**
 * Mapeamento do JSON da API Xtream (`player_api.php`) para as entidades do banco local
 * (ver §3.1 do design). Cada função devolve `null` quando falta o id ou o nome do item —
 * sem isso não há como gravar a linha no banco, então o item é descartado na carga.
 */

fun JsonObject.paraCategoria(contaId: Long, tipo: TipoConteudo): Categoria? {
    val categoriaId = long("category_id") ?: return null
    val nome = texto("category_name") ?: return null
    return Categoria(contaId = contaId, tipo = tipo, categoriaId = categoriaId, nome = nome)
}

fun JsonObject.paraCanal(contaId: Long): Canal? {
    val streamId = long("stream_id") ?: return null
    val nome = texto("name") ?: return null
    return Canal(
        contaId = contaId,
        streamId = streamId,
        nome = nome,
        categoriaId = long("category_id"),
        icone = texto("stream_icon"),
        epgId = texto("epg_channel_id"),
        numero = int("num"),
    )
}

fun JsonObject.paraFilme(contaId: Long): Filme? {
    val streamId = long("stream_id") ?: return null
    val nome = texto("name") ?: return null
    return Filme(
        contaId = contaId,
        streamId = streamId,
        nome = nome,
        categoriaId = long("category_id"),
        capa = texto("stream_icon"),
        extensao = texto("container_extension"),
        ano = int("year"),
        nota = decimal("rating"),
        adicionadoEm = long("added"),
    )
}

fun JsonObject.paraSerie(contaId: Long): Serie? {
    val seriesId = long("series_id") ?: return null
    val nome = texto("name") ?: return null
    return Serie(
        contaId = contaId,
        seriesId = seriesId,
        nome = nome,
        categoriaId = long("category_id"),
        capa = texto("cover"),
        sinopse = texto("plot"),
        ano = int("year"),
        nota = decimal("rating"),
        modificadaEm = long("last_modified"),
    )
}
