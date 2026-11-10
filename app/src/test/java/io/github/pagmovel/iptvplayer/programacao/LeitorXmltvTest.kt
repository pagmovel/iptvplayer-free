package io.github.pagmovel.iptvplayer.programacao

import java.time.Instant
import java.time.ZonedDateTime
import io.github.pagmovel.iptvplayer.dados.Programa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeitorXmltvTest {

    private fun ler(xml: String, janela: LongRange, tamanhoLote: Int = 500): Pair<Int, List<Programa>> {
        val programas = mutableListOf<Programa>()
        val total = lerXmltv(
            entrada = xml.byteInputStream(),
            contaId = 1,
            janela = janela,
            tamanhoLote = tamanhoLote,
            aoLote = { programas.addAll(it) },
        )
        return total to programas
    }

    @Test fun `converte horario com fuso`() {
        val xml = """
            <tv>
            <programme start="20261110030000 +0000" stop="20261110050000 +0000" channel="a.br">
            <title>Jornal</title>
            </programme>
            <programme start="20261110030000 +0300" stop="20261110050000 +0300" channel="a.br">
            <title>Jornal</title>
            </programme>
            </tv>
        """.trimIndent()
        val janela = janelaProgramacao(ZonedDateTime.parse("2026-11-10T12:00:00Z"))

        val (_, programas) = ler(xml, janela)

        assertEquals(2, programas.size)
        assertEquals(Instant.parse("2026-11-10T03:00:00Z").toEpochMilli(), programas[0].inicio)
        assertEquals(Instant.parse("2026-11-10T00:00:00Z").toEpochMilli(), programas[1].inicio)
    }

    @Test fun `ignora programa fora da janela`() {
        val janela = janelaProgramacao(ZonedDateTime.parse("2026-11-10T12:00:00Z")) // de 09/11 a 13/11
        val xml = """
            <tv>
            <programme start="20261001000000 +0000" stop="20261001010000 +0000" channel="a.br">
            <title>Fora</title>
            </programme>
            </tv>
        """.trimIndent()

        val (total, programas) = ler(xml, janela)

        assertEquals(0, total)
        assertTrue(programas.isEmpty())
    }

    @Test fun `aceita programa que comeca antes e termina dentro da janela`() {
        val janela = janelaProgramacao(ZonedDateTime.parse("2026-11-10T12:00:00Z")) // comeca em 09/11 00:00Z
        val xml = """
            <tv>
            <programme start="20261108230000 +0000" stop="20261109010000 +0000" channel="a.br">
            <title>Atravessa</title>
            </programme>
            </tv>
        """.trimIndent()

        val (_, programas) = ler(xml, janela)

        assertEquals(1, programas.size)
        assertEquals("Atravessa", programas[0].titulo)
    }

    @Test fun `titulo ausente vira Sem titulo e entidades sao decodificadas`() {
        val janela = janelaProgramacao(ZonedDateTime.parse("2026-11-10T12:00:00Z"))
        val xml = """
            <tv>
            <programme start="20261110030000 +0000" stop="20261110040000 +0000" channel="a.br">
            </programme>
            <programme start="20261110040000 +0000" stop="20261110050000 +0000" channel="a.br">
            <title>A&amp;E</title>
            </programme>
            </tv>
        """.trimIndent()

        val (_, programas) = ler(xml, janela)

        assertEquals(2, programas.size)
        assertEquals("Sem título", programas[0].titulo)
        assertEquals("A&E", programas[1].titulo)
    }

    @Test fun `ignora tags channel e programa sem channel ou horario invalido`() {
        val janela = janelaProgramacao(ZonedDateTime.parse("2026-11-10T12:00:00Z"))
        val xml = """
            <tv>
            <channel id="a.br">
            <display-name>Canal A</display-name>
            </channel>
            <programme start="20261110030000 +0000" stop="20261110040000 +0000">
            <title>Sem canal</title>
            </programme>
            <programme start="data-invalida" stop="20261110050000 +0000" channel="a.br">
            <title>Horario invalido</title>
            </programme>
            <programme start="20261110050000 +0000" stop="20261110060000 +0000" channel="a.br">
            <title>Valido</title>
            </programme>
            </tv>
        """.trimIndent()

        val (total, programas) = ler(xml, janela)

        assertEquals(1, total)
        assertEquals("Valido", programas[0].titulo)
    }

    @Test fun `entrega em lotes`() {
        val janela = janelaProgramacao(ZonedDateTime.parse("2026-11-10T12:00:00Z"))
        val xml = buildString {
            append("<tv>")
            repeat(1234) { i ->
                append(
                    "<programme start=\"20261110030000 +0000\" stop=\"20261110040000 +0000\" " +
                        "channel=\"canal$i\"><title>P$i</title></programme>",
                )
            }
            append("</tv>")
        }

        val tamanhosDosLotes = mutableListOf<Int>()
        val total = lerXmltv(
            entrada = xml.byteInputStream(),
            contaId = 1,
            janela = janela,
            aoLote = { tamanhosDosLotes.add(it.size) },
        )

        assertEquals(listOf(500, 500, 234), tamanhosDosLotes)
        assertEquals(1234, total)
    }

    @Test fun `janela vai de ontem 0h a depois de amanha 23h59`() {
        val j = janelaProgramacao(ZonedDateTime.parse("2026-11-10T15:00-03:00[America/Sao_Paulo]"))
        assertEquals(
            ZonedDateTime.parse("2026-11-09T00:00-03:00[America/Sao_Paulo]").toInstant().toEpochMilli(),
            j.first,
        )
        assertEquals(
            ZonedDateTime.parse("2026-11-13T00:00-03:00[America/Sao_Paulo]").toInstant().toEpochMilli() - 1,
            j.last,
        )
    }
}
