### T9: Carga do catálogo (orquestração)

**Arquivos:**
- Criar: `carga/CargaCatalogo.kt`
- Teste: `carga/CargaCatalogoTest.kt` (Robolectric, Room em memória, MockWebServer)

**Interfaces:**
- Consome: `XtreamApi.autenticar/lerLista/abrirXmltv` (T5), mapeamentos (T6), `CatalogoDao` e
  `BancoApp.withTransaction` (T7), `lerXmltv`/`janelaProgramacao` (T8).
- Produz: `class CargaCatalogo(private val api: XtreamApi, private val banco: BancoApp, private val contaId: Long, private val agora: () -> ZonedDateTime = { ZonedDateTime.now() })`, com
  `suspend fun executar(progresso: (EtapaCarga) -> Unit = {}): Contagem`.
- Produz: `enum class EtapaCarga { AUTENTICANDO, CATEGORIAS, CANAIS, FILMES, SERIES, PROGRAMACAO, CONCLUIDA }`.
- Ordem: autenticar; numa transação, apagar o catálogo da conta e inserir categorias (os 3
  tipos), canais, filmes e séries em lotes; depois, noutra transação, trocar a programação.
  Falha na programação **não** desfaz o catálogo, porque canal sem programação é aceito pelo
  design.

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `carga completa grava tudo e informa as etapas`()  // contagem == Contagem(3, 2, 1, 4); etapas na ordem do enum
@Test fun `falha nos filmes mantem o catalogo anterior inteiro`() {
    // 1ª carga OK (3 canais); 2ª carga: get_vod_streams devolve 503
    // assertFailsWith<FalhaXtream.ServidorIndisponivel> { carga.executar() }
    // assertEquals(3, dao.contagem(1).canais)  // nada pela metade
}
@Test fun `senha errada nao apaga nada`()
@Test fun `falha so na programacao mantem catalogo novo e programacao antiga`()
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar `executar`.**
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: carga do catálogo em transação, sem perder dados em falha`.

