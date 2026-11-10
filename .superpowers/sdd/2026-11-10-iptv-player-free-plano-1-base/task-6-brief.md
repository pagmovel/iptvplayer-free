### T6: Mapeamento do JSON Xtream para entidades

**Arquivos:**
- Criar: `xtream/JsonCampos.kt`, `xtream/Mapeamento.kt`
- Teste: `xtream/MapeamentoTest.kt`

**Interfaces:**
- Consome: as entidades da T7 (`Categoria`, `Canal`, `Filme`, `Serie`). Escreva a T7 antes, ou
  defina primeiro só as `data class` (sem anotações Room) e complete na T7.
- Produz, em `JsonCampos.kt`: `fun JsonObject.texto(campo: String): String?` (vazio vira `null`),
  `fun JsonObject.long(campo: String): Long?`, `fun JsonObject.int(campo: String): Int?` e
  `fun JsonObject.decimal(campo: String): Double?`. Todos aceitam número, texto numérico, `null`,
  campo ausente e texto vazio.
- Produz, em `Mapeamento.kt`: `fun JsonObject.paraCategoria(contaId: Long, tipo: TipoConteudo): Categoria?`,
  `paraCanal(contaId)`, `paraFilme(contaId)` e `paraSerie(contaId)`. Cada um devolve `null` sem
  id ou nome.

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `numero como texto e como numero`() {
    assertEquals(42L, obj("""{"stream_id":"42"}""").long("stream_id"))
    assertEquals(42L, obj("""{"stream_id":42}""").long("stream_id"))
}
@Test fun `vazio nulo e ausente viram null`() {
    val o = obj("""{"rating":"","category_id":null}""")
    assertNull(o.decimal("rating")); assertNull(o.long("category_id")); assertNull(o.texto("nao_existe"))
}
@Test fun `canal sem categoria e sem icone ainda e importado`() {
    val c = obj("""{"stream_id":7,"name":"Canal Sete","category_id":null,"stream_icon":"","epg_channel_id":"sete.br","num":3}""").paraCanal(1)!!
    assertEquals(Canal(contaId = 1, streamId = 7, nome = "Canal Sete", categoriaId = null, icone = null, epgId = "sete.br", numero = 3), c)
}
@Test fun `filme le extensao ano e nota`()   // container_extension "mkv", year "2019", rating "7.5" → Filme(... extensao="mkv", ano=2019, nota=7.5)
@Test fun `serie le capa sinopse e ultima modificacao`()
@Test fun `item sem id ou sem nome devolve null`()
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar** os auxiliares e os quatro mapeamentos. Os campos vêm da §3.1:
  canal (`stream_id`, `name`, `category_id`, `stream_icon`, `epg_channel_id`, `num`), filme
  (`stream_id`, `name`, `category_id`, `stream_icon`, `container_extension`, `year`, `rating`,
  `added`), série (`series_id`, `name`, `category_id`, `cover`, `plot`, `year`, `rating`,
  `last_modified`) e categoria (`category_id`, `category_name`).
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: mapeamento tolerante do JSON Xtream`.

