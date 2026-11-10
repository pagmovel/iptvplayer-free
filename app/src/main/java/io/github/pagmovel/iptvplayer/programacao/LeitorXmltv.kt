package io.github.pagmovel.iptvplayer.programacao

import io.github.pagmovel.iptvplayer.dados.Programa
import java.io.InputStream
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/** Padrão real do XMLTV para `start`/`stop`: "yyyyMMddHHmmss Z" (ex.: "20261110030000 +0000").
 * Alguns arquivos do provedor também mandam `start_timestamp`/`stop_timestamp`, mas nem
 * todo arquivo tem esses atributos — por isso o leitor usa só `start`/`stop`. */
private val FORMATO_DATA: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z")

/**
 * Janela padrão de carga da programação: de ontem 00:00 até depois de amanhã 23:59:59.999,
 * sempre no fuso de [agora] (nunca o fuso do sistema) — para o teste poder fixar um fuso e
 * uma data e o resultado ser sempre o mesmo.
 */
fun janelaProgramacao(agora: ZonedDateTime): LongRange {
    val hoje = agora.toLocalDate()
    val inicio = hoje.minusDays(1).atStartOfDay(agora.zone).toInstant().toEpochMilli()
    val fim = hoje.plusDays(3).atStartOfDay(agora.zone).toInstant().toEpochMilli() - 1
    return inicio..fim
}

/**
 * Lê o XMLTV em fluxo (nunca carrega o arquivo inteiro em memória — o arquivo real do
 * provedor tem 8,2 MB e pode crescer) e entrega os `<programme>` dentro de [janela] em lotes
 * de [tamanhoLote] via [aoLote]. Devolve o total de programas aceitos.
 *
 * Um programa é aceito quando tem `channel`, `start` e `stop` válidos e o intervalo
 * `[start, stop]` tem alguma intersecção com [janela] (um programa que começa antes e
 * termina dentro da janela também é aceito). `<channel>` e qualquer atributo/tag
 * desconhecido são ignorados.
 */
fun lerXmltv(
    entrada: InputStream,
    contaId: Long,
    janela: LongRange,
    tamanhoLote: Int = 500,
    aoLote: (List<Programa>) -> Unit,
): Int {
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    // encoding null: deixa o parser detectar pelo BOM/prolog do próprio XML, em vez de
    // supor UTF-8 fixo.
    parser.setInput(entrada, null)

    var total = 0
    val lote = ArrayList<Programa>(tamanhoLote)

    var evento = parser.eventType
    while (evento != XmlPullParser.END_DOCUMENT) {
        if (evento == XmlPullParser.START_TAG && parser.name == "programme") {
            lerPrograma(parser, contaId, janela)?.let { programa ->
                lote.add(programa)
                if (lote.size >= tamanhoLote) {
                    aoLote(ArrayList(lote))
                    total += lote.size
                    lote.clear()
                }
            }
        }
        evento = parser.next()
    }
    if (lote.isNotEmpty()) {
        aoLote(ArrayList(lote))
        total += lote.size
    }
    return total
}

/**
 * Lê um `<programme>` já posicionado no evento START_TAG (para poder ler os atributos) e
 * consome toda a sua subárvore, inclusive quando é descartado — sem isso o parser ficaria
 * desalinhado e o próximo `next()` do chamador leria um filho do programa em vez do próximo
 * irmão. Devolve `null` quando falta `channel`/horário válido ou o programa está fora da
 * [janela].
 */
private fun lerPrograma(parser: XmlPullParser, contaId: Long, janela: LongRange): Programa? {
    val epgId = parser.getAttributeValue(null, "channel")
    val inicio = analisarInstante(parser.getAttributeValue(null, "start"))
    val fim = analisarInstante(parser.getAttributeValue(null, "stop"))

    var titulo: String? = null
    var descricao: String? = null

    while (true) {
        when (parser.next()) {
            XmlPullParser.START_TAG -> when (parser.name) {
                "title" -> titulo = lerTextoEFechar(parser)
                "desc" -> descricao = lerTextoEFechar(parser)
                else -> pularSubarvore(parser)
            }
            XmlPullParser.END_TAG -> return montar(contaId, epgId, inicio, fim, titulo, descricao, janela)
            XmlPullParser.END_DOCUMENT -> return montar(contaId, epgId, inicio, fim, titulo, descricao, janela)
        }
    }
}

private fun montar(
    contaId: Long,
    epgId: String?,
    inicio: Long?,
    fim: Long?,
    titulo: String?,
    descricao: String?,
    janela: LongRange,
): Programa? {
    if (epgId == null || inicio == null || fim == null) return null
    if (inicio > janela.last || fim < janela.first) return null
    return Programa(
        contaId = contaId,
        epgId = epgId,
        inicio = inicio,
        fim = fim,
        titulo = titulo ?: "Sem título",
        descricao = descricao,
    )
}

/** Lê o texto de um elemento simples (ex.: `<title>…</title>`) e consome até o seu END_TAG. */
private fun lerTextoEFechar(parser: XmlPullParser): String {
    val texto = StringBuilder()
    var profundidade = 1
    while (profundidade > 0) {
        when (parser.next()) {
            XmlPullParser.TEXT -> texto.append(parser.text)
            XmlPullParser.START_TAG -> profundidade++
            XmlPullParser.END_TAG -> profundidade--
        }
    }
    return texto.toString()
}

/** Pula um elemento e toda a sua subárvore (ex.: `<icon src="…"/>`, `<category>…</category>`). */
private fun pularSubarvore(parser: XmlPullParser) {
    var profundidade = 1
    while (profundidade > 0) {
        when (parser.next()) {
            XmlPullParser.START_TAG -> profundidade++
            XmlPullParser.END_TAG -> profundidade--
        }
    }
}

/** Converte `start`/`stop` no padrão "yyyyMMddHHmmss Z"; `null` se faltar ou for inválido. */
private fun analisarInstante(texto: String?): Long? {
    if (texto == null) return null
    return try {
        OffsetDateTime.parse(texto, FORMATO_DATA).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }
}
