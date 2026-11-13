### T5: Cascata de qualidade (máquina de estados pura)

Núcleo da troca automática/manual — **sem Android, sem ExoPlayer**, só lógica e
`kotlinx-coroutines-test` para o tempo, testável rápido e sem Robolectric.

**Arquivos:**
- Criar: `aovivo/CascataQualidade.kt`
- Teste: `aovivo/CascataQualidadeTest.kt`

**Interfaces:**
- Consome: `CanalAgrupado`, `VarianteQualidade`, `Qualidade` (T1); `tempoBufferMs` (T4, passado
  por parâmetro — esta classe não lê `ConfiguracoesRepositorio` diretamente, pra não depender
  de Android nos testes).
- Produz:
  - `sealed class EventoCascata { data class Tocando(val variante: VarianteQualidade): EventoCascata(); data class Trocou(val de: Qualidade, val para: Qualidade): EventoCascata(); object FalhaTerminal: EventoCascata() }`
  - `class CascataQualidade(private val canal: CanalAgrupado, private val tempoBufferMs: Long, private val escopo: CoroutineScope)` com:
    - `val eventos: Flow<EventoCascata>`
    - `fun iniciar()` — começa pela variante de maior qualidade, emite `Tocando`.
    - `fun aoTravar()` — chamado quando o player entra em buffering; inicia (ou reinicia, se
      já ativo) o timer de `tempoBufferMs`; se o timer expirar sem `aoEstabilizar()`, desce
      uma qualidade (ou `FalhaTerminal` se já estava na pior) e emite `Trocou`/`FalhaTerminal`.
    - `fun aoEstabilizar()` — cancela o timer pendente (reproduzindo bem).
    - `fun aoErroDefinitivo()` — desce uma qualidade **na hora**, sem esperar timer (ou
      `FalhaTerminal` se já estava na pior).
    - `fun escolherManualmente(qualidade: Qualidade)` — muda a variante atual sem mexer no
      timer; a cascata automática continua valendo a partir daí.
    - `fun tentarNovamente()` — só válido depois de `FalhaTerminal`; reinicia da maior
      qualidade de novo.

- [ ] **Passo 1: Escrever os testes que falham** (usar `runTest` do `kotlinx-coroutines-test`
  para controlar o tempo sem esperar de verdade):

```kotlin
@Test fun `inicia sempre na maior qualidade disponivel`()
// CanalAgrupado com [FHD, HD, SD] -> iniciar() emite Tocando(FHD).

@Test fun `trava por mais que o tempo configurado desce uma qualidade e reinicia o timer`()
// iniciar() (FHD); aoTravar(); advanceTimeBy(tempoBufferMs); emite Trocou(FHD, HD).
// aoTravar() de novo; advanceTimeBy(tempoBufferMs/2); nao emite nada ainda (timer reiniciou).

@Test fun `travar por menos que o tempo configurado nao troca`()
// aoTravar(); advanceTimeBy(tempoBufferMs - 1); aoEstabilizar(); nenhum Trocou emitido.

@Test fun `erro definitivo troca na hora, sem esperar o timer`()
// aoErroDefinitivo() emite Trocou(...) imediatamente, sem advanceTimeBy.

@Test fun `canal com 1 variante so vai direto pra falha terminal ao travar`()
// CanalAgrupado com 1 variante só; aoTravar() + tempo todo -> emite FalhaTerminal, nao Trocou.

@Test fun `se a pior qualidade tambem falhar, emite falha terminal e para`()
// chega no SD (pior), trava de novo -> FalhaTerminal; nenhum evento Trocou depois disso
// sem um tentarNovamente() explicito.

@Test fun `escolha manual muda a variante sem desligar a cascata automatica`()
// iniciar() (FHD); escolherManualmente(SD); aoTravar() + tempo -> FalhaTerminal (SD era a
// pior) -- prova que a automatica continua agindo a partir da escolha manual.

@Test fun `tentarNovamente apos falha terminal reinicia da maior qualidade`()
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar** com `kotlinx.coroutines.Job`/`delay(tempoBufferMs)` lançado em
  `escopo`; `aoTravar()` cancela o job anterior antes de lançar um novo (isso é o "reinicia o
  timer"); `aoEstabilizar()` só cancela, sem lançar nada.
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: maquina de estados da troca automatica/manual de qualidade`

