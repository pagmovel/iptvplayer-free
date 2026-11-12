package io.github.pagmovel.iptvplayer.aovivo

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val TEMPO_BUFFER_MS = 8_000L

private fun canalCom(vararg qualidades: Qualidade): CanalAgrupado {
    val variantes = qualidades.mapIndexed { indice, qualidade ->
        VarianteQualidade(qualidade, streamId = (indice + 1).toLong())
    }
    return CanalAgrupado(
        nomeBase = "CANAL TESTE",
        categoriaId = 1,
        icone = null,
        epgId = null,
        numero = null,
        variantes = variantes,
    )
}

class CascataQualidadeTest {

    /** Coleta os eventos num coroutine de fundo com dispatcher nao-confinado, pra o collector
     * ja estar inscrito (suspenso aguardando) antes de qualquer emissao do teste -- sem isso
     * o tryEmit do SharedFlow nao tem pra quem entregar o evento e ele se perde. */
    private fun TestScope.coletar(cascata: CascataQualidade): MutableList<EventoCascata> {
        val eventos = mutableListOf<EventoCascata>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cascata.eventos.toList(eventos)
        }
        return eventos
    }

    @Test
    fun `inicia sempre na maior qualidade disponivel`() = runTest {
        val canal = canalCom(Qualidade.FHD, Qualidade.HD, Qualidade.SD)
        val cascata = CascataQualidade(canal, TEMPO_BUFFER_MS, this)
        val eventos = coletar(cascata)

        cascata.iniciar()

        assertEquals(listOf(EventoCascata.Tocando(canal.variantes[0])), eventos)
    }

    @Test
    fun `trava por mais que o tempo configurado desce uma qualidade e reinicia o timer`() = runTest {
        val canal = canalCom(Qualidade.FHD, Qualidade.HD, Qualidade.SD)
        val cascata = CascataQualidade(canal, TEMPO_BUFFER_MS, this)
        val eventos = coletar(cascata)

        cascata.iniciar()
        cascata.aoTravar()
        advanceTimeBy(TEMPO_BUFFER_MS)
        runCurrent()

        assertEquals(EventoCascata.Trocou(Qualidade.FHD, Qualidade.HD), eventos.last())

        cascata.aoTravar()
        advanceTimeBy(TEMPO_BUFFER_MS / 2)

        // timer reiniciou do zero na segunda chamada: metade do tempo nao basta
        assertEquals(2, eventos.size)
    }

    @Test
    fun `travar por menos que o tempo configurado nao troca`() = runTest {
        val canal = canalCom(Qualidade.FHD, Qualidade.HD, Qualidade.SD)
        val cascata = CascataQualidade(canal, TEMPO_BUFFER_MS, this)
        val eventos = coletar(cascata)

        cascata.iniciar()
        cascata.aoTravar()
        advanceTimeBy(TEMPO_BUFFER_MS - 1)
        cascata.aoEstabilizar()
        advanceTimeBy(TEMPO_BUFFER_MS)
        runCurrent()

        assertTrue(eventos.none { it is EventoCascata.Trocou })
    }

    @Test
    fun `erro definitivo troca na hora, sem esperar o timer`() = runTest {
        val canal = canalCom(Qualidade.FHD, Qualidade.HD, Qualidade.SD)
        val cascata = CascataQualidade(canal, TEMPO_BUFFER_MS, this)
        val eventos = coletar(cascata)

        cascata.iniciar()
        cascata.aoErroDefinitivo()

        assertEquals(EventoCascata.Trocou(Qualidade.FHD, Qualidade.HD), eventos.last())
    }

    @Test
    fun `canal com 1 variante so vai direto pra falha terminal ao travar`() = runTest {
        val canal = canalCom(Qualidade.SD)
        val cascata = CascataQualidade(canal, TEMPO_BUFFER_MS, this)
        val eventos = coletar(cascata)

        cascata.iniciar()
        cascata.aoTravar()
        advanceTimeBy(TEMPO_BUFFER_MS)
        runCurrent()

        assertEquals(EventoCascata.FalhaTerminal, eventos.last())
        assertTrue(eventos.none { it is EventoCascata.Trocou })
    }

    @Test
    fun `se a pior qualidade tambem falhar, emite falha terminal e para`() = runTest {
        val canal = canalCom(Qualidade.FHD, Qualidade.HD, Qualidade.SD)
        val cascata = CascataQualidade(canal, TEMPO_BUFFER_MS, this)
        val eventos = coletar(cascata)

        cascata.iniciar()
        cascata.aoTravar()
        advanceTimeBy(TEMPO_BUFFER_MS)
        runCurrent() // FHD -> HD
        cascata.aoTravar()
        advanceTimeBy(TEMPO_BUFFER_MS)
        runCurrent() // HD -> SD (pior)
        cascata.aoTravar()
        advanceTimeBy(TEMPO_BUFFER_MS)
        runCurrent() // SD tambem falha -> FalhaTerminal

        assertEquals(EventoCascata.FalhaTerminal, eventos.last())
        val quantidadeAntes = eventos.size

        // a cascata para: sem tentarNovamente(), nao insiste sozinha
        cascata.aoTravar()
        advanceTimeBy(TEMPO_BUFFER_MS)
        runCurrent()

        assertEquals(quantidadeAntes, eventos.size)
    }

    @Test
    fun `escolha manual muda a variante sem desligar a cascata automatica`() = runTest {
        val canal = canalCom(Qualidade.FHD, Qualidade.HD, Qualidade.SD)
        val cascata = CascataQualidade(canal, TEMPO_BUFFER_MS, this)
        val eventos = coletar(cascata)

        cascata.iniciar()
        cascata.escolherManualmente(Qualidade.SD)
        cascata.aoTravar()
        advanceTimeBy(TEMPO_BUFFER_MS)
        runCurrent()

        // prova que a automatica continua agindo a partir da escolha manual: SD era a pior
        assertEquals(EventoCascata.FalhaTerminal, eventos.last())
    }

    @Test
    fun `escolha manual emite Trocou na hora com a variante escolhida`() = runTest {
        val canal = canalCom(Qualidade.FHD, Qualidade.HD, Qualidade.SD)
        val cascata = CascataQualidade(canal, TEMPO_BUFFER_MS, this)
        val eventos = coletar(cascata)

        cascata.iniciar()
        cascata.escolherManualmente(Qualidade.SD)

        // sem isso o player (que só reage a `eventos`) nunca trocaria de fato o que está
        // tocando -- achado real da T9, ver comentário em escolherManualmente
        assertEquals(EventoCascata.Trocou(Qualidade.FHD, Qualidade.SD), eventos.last())
    }

    @Test
    fun `escolher a qualidade que ja esta tocando nao emite evento`() = runTest {
        val canal = canalCom(Qualidade.FHD, Qualidade.HD, Qualidade.SD)
        val cascata = CascataQualidade(canal, TEMPO_BUFFER_MS, this)
        val eventos = coletar(cascata)

        cascata.iniciar()
        cascata.escolherManualmente(Qualidade.FHD)

        assertEquals(listOf(EventoCascata.Tocando(canal.variantes[0])), eventos)
    }

    @Test
    fun `tentarNovamente apos falha terminal reinicia da maior qualidade`() = runTest {
        val canal = canalCom(Qualidade.FHD, Qualidade.HD, Qualidade.SD)
        val cascata = CascataQualidade(canal, TEMPO_BUFFER_MS, this)
        val eventos = coletar(cascata)

        cascata.iniciar()
        cascata.escolherManualmente(Qualidade.SD)
        cascata.aoTravar()
        advanceTimeBy(TEMPO_BUFFER_MS)
        runCurrent() // FalhaTerminal

        cascata.tentarNovamente()

        assertEquals(EventoCascata.Tocando(canal.variantes[0]), eventos.last())
    }

    @Test
    fun `escolha manual depois de falha terminal reativa a cascata automatica`() = runTest {
        val canal = canalCom(Qualidade.HD, Qualidade.SD)
        val cascata = CascataQualidade(canal, TEMPO_BUFFER_MS, this)
        val eventos = coletar(cascata)

        cascata.iniciar()
        cascata.aoTravar()
        advanceTimeBy(TEMPO_BUFFER_MS)
        runCurrent() // HD -> SD
        cascata.aoTravar()
        advanceTimeBy(TEMPO_BUFFER_MS)
        runCurrent() // SD e a pior -> FalhaTerminal

        assertEquals(EventoCascata.FalhaTerminal, eventos.last())

        cascata.escolherManualmente(Qualidade.HD)
        cascata.aoTravar()
        advanceTimeBy(TEMPO_BUFFER_MS)
        runCurrent()

        // a automatica voltou a agir a partir da escolha manual: degrada de novo em vez de
        // ficar travada no estado terminal antigo
        assertEquals(EventoCascata.Trocou(Qualidade.HD, Qualidade.SD), eventos.last())
    }
}
