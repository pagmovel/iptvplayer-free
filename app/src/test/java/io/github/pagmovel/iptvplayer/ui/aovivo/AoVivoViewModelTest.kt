package io.github.pagmovel.iptvplayer.ui.aovivo

import io.github.pagmovel.iptvplayer.aovivo.CanalAgrupado
import io.github.pagmovel.iptvplayer.aovivo.Qualidade
import io.github.pagmovel.iptvplayer.aovivo.VarianteQualidade
import io.github.pagmovel.iptvplayer.dados.CanalFavorito
import io.github.pagmovel.iptvplayer.dados.Programa
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** So a logica propria da T7 (filtro de canais e escolha do programa no ar) e da T8 (janela
 * do dia da programacao) -- o resto de [AoVivoViewModel] so repassa chamadas pra DAO/
 * repositorios/cascata/player, ja cobertos pelos testes deles (T1/T2/T3/T5/T6). */
class AoVivoViewModelTest {

    private fun canal(nomeBase: String, categoriaId: Long) = CanalAgrupado(
        nomeBase = nomeBase,
        categoriaId = categoriaId,
        icone = null,
        epgId = null,
        numero = null,
        variantes = listOf(VarianteQualidade(Qualidade.SD, 1L)),
    )

    @Test
    fun `sem filtro nenhum devolve todos os canais`() {
        val canais = listOf(canal("A", 1), canal("B", 2))
        val resultado = filtrarCanais(canais, favoritos = emptyList(), categoriaId = null, somenteFavoritos = false)
        assertEquals(2, resultado.size)
    }

    @Test
    fun `filtro por categoria so devolve canais daquela categoria`() {
        val canais = listOf(canal("A", 1), canal("B", 2))
        val resultado = filtrarCanais(canais, favoritos = emptyList(), categoriaId = 2L, somenteFavoritos = false)
        assertEquals(listOf("B"), resultado.map { it.nomeBase })
    }

    @Test
    fun `filtro so favoritos usa nomeBase mais categoriaId como chave`() {
        val canais = listOf(canal("A", 1), canal("B", 1))
        val favoritos = listOf(CanalFavorito(contaId = 1, nomeBase = "A", categoriaId = 1, criadoEm = 0))
        val resultado = filtrarCanais(canais, favoritos, categoriaId = null, somenteFavoritos = true)
        assertEquals(listOf("A"), resultado.map { it.nomeBase })
    }

    @Test
    fun `categoria e favoritos combinam com AND`() {
        val canais = listOf(canal("A", 1), canal("A", 2))
        val favoritos = listOf(CanalFavorito(contaId = 1, nomeBase = "A", categoriaId = 1, criadoEm = 0))
        // Favorito e da categoria 1; filtrar por categoria 2 nao deve trazer nada mesmo com somenteFavoritos=false
        val resultado = filtrarCanais(canais, favoritos, categoriaId = 2L, somenteFavoritos = true)
        assertEquals(emptyList<String>(), resultado.map { it.nomeBase })
    }

    private fun programa(inicio: Long, fim: Long) =
        Programa(contaId = 1, epgId = "epg1", inicio = inicio, fim = fim, titulo = "t-$inicio", descricao = null)

    @Test
    fun `programaNoInstante acha o programa cujo fim ainda nao passou`() {
        val programas = listOf(programa(0, 100), programa(100, 200), programa(200, 300))
        val resultado = programaNoInstante(programas, agora = 150)
        assertEquals("t-100", resultado?.titulo)
    }

    @Test
    fun `programaNoInstante devolve null quando o ultimo programa da janela ja terminou`() {
        val programas = listOf(programa(0, 100), programa(100, 200))
        val resultado = programaNoInstante(programas, agora = 250)
        assertNull(resultado)
    }

    @Test
    fun `programaNoInstante devolve null para lista vazia`() {
        assertNull(programaNoInstante(emptyList(), agora = 150))
    }

    private fun calendarEm(ano: Int, mes: Int, dia: Int, hora: Int, minuto: Int): Long {
        val calendario = Calendar.getInstance()
        calendario.set(ano, mes, dia, hora, minuto, 0)
        calendario.set(Calendar.MILLISECOND, 0)
        return calendario.timeInMillis
    }

    @Test
    fun `janelaDoDia HOJE cobre de 00h00 a 00h00 do dia seguinte`() {
        val agora = calendarEm(2026, Calendar.NOVEMBER, 12, 15, 30)
        val (inicio, fim) = janelaDoDia(DiaPrograma.HOJE, agora)
        assertEquals(calendarEm(2026, Calendar.NOVEMBER, 12, 0, 0), inicio)
        assertEquals(calendarEm(2026, Calendar.NOVEMBER, 13, 0, 0), fim)
    }

    @Test
    fun `janelaDoDia ONTEM desloca a janela um dia pra tras`() {
        val agora = calendarEm(2026, Calendar.NOVEMBER, 12, 15, 30)
        val (inicio, fim) = janelaDoDia(DiaPrograma.ONTEM, agora)
        assertEquals(calendarEm(2026, Calendar.NOVEMBER, 11, 0, 0), inicio)
        assertEquals(calendarEm(2026, Calendar.NOVEMBER, 12, 0, 0), fim)
    }

    @Test
    fun `janelaDoDia AMANHA desloca a janela um dia pra frente`() {
        val agora = calendarEm(2026, Calendar.NOVEMBER, 12, 15, 30)
        val (inicio, fim) = janelaDoDia(DiaPrograma.AMANHA, agora)
        assertEquals(calendarEm(2026, Calendar.NOVEMBER, 13, 0, 0), inicio)
        assertEquals(calendarEm(2026, Calendar.NOVEMBER, 14, 0, 0), fim)
    }

    @Test
    fun `janelaDoDia funciona perto da virada do dia (23h59)`() {
        val agora = calendarEm(2026, Calendar.NOVEMBER, 12, 23, 59)
        val (inicio, fim) = janelaDoDia(DiaPrograma.HOJE, agora)
        assertEquals(calendarEm(2026, Calendar.NOVEMBER, 12, 0, 0), inicio)
        assertEquals(calendarEm(2026, Calendar.NOVEMBER, 13, 0, 0), fim)
    }
}
