### T7: Banco local (Room + FTS)

**Arquivos:**
- Criar: `dados/Entidades.kt`, `dados/Fts.kt`, `dados/CatalogoDao.kt`, `dados/BancoApp.kt`
- Teste: `dados/CatalogoDaoTest.kt` (Robolectric, banco em memória)

**Interfaces:**
- Produz: `enum class TipoConteudo { AO_VIVO, FILME, SERIE }`.
- Produz, as entidades (chave primária composta com `contaId`):
  - `Categoria(contaId: Long, tipo: TipoConteudo, categoriaId: Long, nome: String)`
  - `Canal(contaId: Long, streamId: Long, nome: String, categoriaId: Long?, icone: String?, epgId: String?, numero: Int?)`
  - `Filme(contaId: Long, streamId: Long, nome: String, categoriaId: Long?, capa: String?, extensao: String?, ano: Int?, nota: Double?, adicionadoEm: Long?)`
  - `Serie(contaId: Long, seriesId: Long, nome: String, categoriaId: Long?, capa: String?, sinopse: String?, ano: Int?, nota: Double?, modificadaEm: Long?)`
  - `Programa(contaId: Long, epgId: String, inicio: Long, fim: Long, titulo: String, descricao: String?)`
    (epoch ms; índice em `contaId, epgId, inicio`)
  - `Conta(id: Long = 0, nome: String, servidor: String, usuario: String, senhaCifrada: ByteArray, ativa: Boolean, ultimaCargaEm: Long?)`
- Produz: tabelas FTS4 com `contentEntity` para `Canal.nome`, `Filme.nome` e `Serie.nome`,
  usando `tokenizer = FtsOptions.TOKENIZER_UNICODE61` (remove acentos).
- Produz: `CatalogoDao` com
  - `suspend fun inserirCategorias(l: List<Categoria>)`; idem `inserirCanais`, `inserirFilmes`,
    `inserirSeries` e `inserirProgramas`;
  - `suspend fun apagarCatalogo(contaId: Long)` (categorias, canais, filmes e séries) e
    `suspend fun apagarProgramacao(contaId: Long)`;
  - `suspend fun contagem(contaId: Long): Contagem`, com
    `data class Contagem(val canais: Int, val filmes: Int, val series: Int, val programas: Int)`;
  - `suspend fun buscarNomesCanais(contaId: Long, termo: String): List<Canal>`, para provar o FTS
    agora (a busca completa vem no Plano 3).
- Produz: `BancoApp` (`RoomDatabase`, versão 1, `exportSchema = true`, esquema em
  `app/schemas/`) com `fun catalogoDao()` e `fun contaDao()`. O `ContaDao` é preenchido na T10.
  Toda a troca de catálogo acontece dentro de `banco.withTransaction { }`, chamada pela T9.

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `busca ignora acento e maiusculas`() = runTest {
    dao.inserirCanais(listOf(canal(1, "São Paulo HD"), canal(2, "Rio FHD")))
    assertEquals(listOf("São Paulo HD"), dao.buscarNomesCanais(contaId = 1, termo = "sao paulo").map { it.nome })
    assertEquals(listOf("São Paulo HD"), dao.buscarNomesCanais(1, "SÃO").map { it.nome })
}
@Test fun `busca por prefixo enquanto digita`()       // "pau" acha "São Paulo HD"
@Test fun `contas nao se misturam`()                  // canal da conta 2 não aparece na busca nem na contagem da conta 1
@Test fun `apagarCatalogo limpa so a conta indicada`()
@Test fun `contagem soma cada tipo`()
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar.** Para o termo do FTS, separe por espaço, escape aspas e junte como
  `"palavra"*` com AND implícito, para a busca por prefixo funcionar. Ligue a tabela FTS à
  entidade por `rowid`. Ative `ksp` com `room.schemaLocation`.
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: banco local com busca FTS sem acento`.

