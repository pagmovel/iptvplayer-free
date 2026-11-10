package io.github.pagmovel.iptvplayer.xtream

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Leitura tolerante de campos do JSON da API Xtream: o provedor mistura número e texto no
 * mesmo campo (ex.: "stream_id": 42 ou "stream_id": "42"), manda `null`, omite o campo ou
 * manda texto vazio (ex.: "rating": ""). Todo auxiliar aqui devolve `null` nesses casos em
 * vez de lançar exceção — dado real do provedor vem inconsistente.
 */

private fun JsonObject.primitivoOuNull(campo: String): JsonPrimitive? {
    val valor = this[campo] ?: return null
    if (valor is JsonNull) return null
    val primitivo = valor as? JsonPrimitive ?: return null
    if (primitivo.isString && primitivo.content.isEmpty()) return null
    return primitivo
}

fun JsonObject.texto(campo: String): String? = primitivoOuNull(campo)?.content

fun JsonObject.long(campo: String): Long? = primitivoOuNull(campo)?.content?.toLongOrNull()

fun JsonObject.int(campo: String): Int? = primitivoOuNull(campo)?.content?.toIntOrNull()

fun JsonObject.decimal(campo: String): Double? = primitivoOuNull(campo)?.content?.toDoubleOrNull()
