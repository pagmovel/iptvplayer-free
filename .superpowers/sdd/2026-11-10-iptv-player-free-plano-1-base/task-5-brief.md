### T5: Cliente da API Xtream

**Arquivos:**
- Criar: `xtream/XtreamApi.kt`, `xtream/FalhaXtream.kt`
- Teste: `xtream/XtreamApiTest.kt` (MockWebServer)

**Interfaces:**
- Consome: `CredenciaisXtream` (T4).
- Produz: `class XtreamApi(private val cliente: OkHttpClient, private val cred: CredenciaisXtream)`, com:
  - `suspend fun autenticar(): InfoConta`, onde
    `data class InfoConta(val ativa: Boolean, val expiraEm: Long?, val maxConexoes: Int?)`
    (`expiraEm` em epoch segundos);
  - `suspend fun lerLista(acao: String, tamanhoLote: Int = 500, aoLote: suspend (List<JsonObject>) -> Unit): Int`,
    que devolve o total lido;
  - `suspend fun <T> abrirXmltv(ler: suspend (InputStream) -> T): T`;
  - `fun urlAoVivo(streamId: Long): String` → `"$servidor/live/$usuario/$senha/$streamId.ts"`;
    `fun urlFilme(streamId: Long, extensao: String)` → `/movie/...`;
    `fun urlEpisodio(id: Long, extensao: String)` → `/series/...`.
- Produz: `sealed class FalhaXtream : Exception`, com os casos `CredenciaisInvalidas`,
  `ContaInativa(status: String)`, `ServidorIndisponivel(causa: Throwable?)` e
  `RespostaInvalida(detalhe: String)`.

- [ ] **Passo 1: Escrever os testes que falham.** Use JSON sintético no formato da §3.1.

```kotlin
@Test fun `autenticar le conta ativa`()            // {"user_info":{"auth":1,"status":"Active","exp_date":"1800000000","max_connections":"2"},...}
    // assertEquals(InfoConta(true, 1800000000, 2), api.autenticar()); path == "/player_api.php?username=u&password=p"
@Test fun `auth 0 vira CredenciaisInvalidas`()       // {"user_info":{"auth":0}}
@Test fun `status Expired vira ContaInativa`()       // assertEquals("Expired", (falha as ContaInativa).status)
@Test fun `HTTP 503 e timeout viram ServidorIndisponivel`()
@Test fun `resposta HTML vira RespostaInvalida`()    // corpo "<!doctype html>…", como o redirect do get.php
@Test fun `lerLista entrega em lotes sem passar do tamanho`() {
    // servidor devolve um array JSON com 1234 objetos {"stream_id":N,"name":"Canal N"}
    // lotes = mutableListOf<Int>(); total = api.lerLista("get_live_streams", 500) { lotes += it.size }
    // assertEquals(listOf(500, 500, 234), lotes); assertEquals(1234, total)
    // path termina em "&action=get_live_streams"
}
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar.** Use OkHttp com `callTimeout` de 120 s para listas e 20 s para
  autenticar. Não siga redirecionamentos para outro host (`followRedirects(false)` no cliente da
  API): um 3xx vira `RespostaInvalida`. `lerLista` usa
  `Json.decodeToSequence<JsonObject>(corpo.byteStream(), DecodeSequenceMode.ARRAY_WRAPPED)`. Os
  números vêm como texto ou número: leia-os com os auxiliares da T6 (`JsonObject.long`/`int`).
  Implemente a T6 primeiro se precisar, ou declare esses auxiliares aqui e mova-os depois. Rode
  o I/O em `Dispatchers.IO`.
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: cliente da API Xtream com leitura em fluxo`.

