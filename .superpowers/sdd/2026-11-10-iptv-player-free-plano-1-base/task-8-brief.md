### T8: Leitor de XMLTV

**Arquivos:**
- Criar: `programacao/LeitorXmltv.kt`
- Teste: `programacao/LeitorXmltvTest.kt` (JVM com `kxml2` como `testImplementation`)

**Interfaces:**
- Consome: `Programa` (T7).
- Produz: `fun lerXmltv(entrada: InputStream, contaId: Long, janela: LongRange, tamanhoLote: Int = 500, aoLote: (List<Programa>) -> Unit): Int`,
  que devolve o total aceito. `janela` em epoch ms.
- Produz: `fun janelaProgramacao(agora: ZonedDateTime): LongRange`, de ontem 00:00 até depois de
  amanhã 23:59:59.999, no fuso de `agora`.

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `converte horario com fuso`() {
    // <programme start="20261110030000 +0000" stop="20261110050000 +0000" channel="a.br"><title>Jornal</title></programme>
    // assertEquals(Instant.parse("2026-11-10T03:00:00Z").toEpochMilli(), p.inicio)
    // idem com "+0300" → 2026-11-10T00:00:00Z
}
@Test fun `ignora programa fora da janela`()
@Test fun `aceita programa que comeca antes e termina dentro da janela`()
@Test fun `titulo ausente vira Sem titulo e entidades sao decodificadas`()  // "A&amp;E" → "A&E"
@Test fun `ignora tags channel e programa sem channel ou horario invalido`()
@Test fun `entrega em lotes`()   // 1234 programas na janela → lotes 500, 500, 234
@Test fun `janela vai de ontem 0h a depois de amanha 23h59`() {
    val j = janelaProgramacao(ZonedDateTime.parse("2026-11-10T15:00-03:00[America/Sao_Paulo]"))
    assertEquals(ZonedDateTime.parse("2026-11-09T00:00-03:00[America/Sao_Paulo]").toInstant().toEpochMilli(), j.first)
    assertEquals(ZonedDateTime.parse("2026-11-13T00:00-03:00[America/Sao_Paulo]").toInstant().toEpochMilli() - 1, j.last)
}
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar** com `XmlPullParserFactory.newInstance().newPullParser()`. Leia
  `start`/`stop` pelo padrão `yyyyMMddHHmmss Z` com `DateTimeFormatter`, que funciona no minSdk 26
  sem desugaring. Ignore os atributos `start_timestamp`, porque nem todo XMLTV os tem. Use só os
  filhos `title` e `desc`.
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: leitor de XMLTV em fluxo com janela de 4 dias`.

