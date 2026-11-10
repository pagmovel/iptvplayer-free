### T4: Leitura do link do provedor

**Arquivos:**
- Criar: `conta/LinkXtream.kt`
- Teste: `conta/LinkXtreamTest.kt`

**Interfaces:**
- Produz: `data class CredenciaisXtream(val servidor: String, val usuario: String, val senha: String)`.
  `servidor` = esquema + host + porta (se houver), sem barra final, ex. `http://exemplo.tv:80`.
- Produz: `fun lerLinkXtream(texto: String): CredenciaisXtream?`

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `extrai do link no formato que o provedor manda`() {
    val texto = "🟢 *Link Dns (M3U) :\n\n http://exemplo.tv:80/get.php?username=joao&password=s3nh4&type=m3u_plus&output=mpegts\n"
    assertEquals(CredenciaisXtream("http://exemplo.tv:80", "joao", "s3nh4"), lerLinkXtream(texto))
}
@Test fun `aceita link da player_api e sem porta`() {
    assertEquals(CredenciaisXtream("http://exemplo.tv", "a", "b"),
        lerLinkXtream("http://exemplo.tv/player_api.php?username=a&password=b"))
}
@Test fun `decodifica caracteres codificados na senha`() {
    assertEquals("a+b&c", lerLinkXtream("http://x.tv/get.php?username=u&password=a%2Bb%26c")!!.senha)
}
@Test fun `devolve null sem usuario ou senha ou sem link`() {
    assertNull(lerLinkXtream("http://x.tv/get.php?username=u"))
    assertNull(lerLinkXtream("texto qualquer"))
    assertNull(lerLinkXtream(""))
}
```

- [ ] **Passo 2: Rodar e ver falhar** (`--tests '*LinkXtreamTest'`).
- [ ] **Passo 3: Implementar `lerLinkXtream`.** Ache o primeiro `https?://\S+` no texto e
  interprete-o com `java.net.URI`. Decodifique os parâmetros com `URLDecoder` em UTF-8. Não use
  `android.net.Uri`, que não funciona em teste na JVM sem Robolectric.
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: leitura de credenciais a partir do link do provedor`.

