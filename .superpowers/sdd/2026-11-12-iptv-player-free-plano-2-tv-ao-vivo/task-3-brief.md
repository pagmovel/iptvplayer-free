### T3: Consultas do catálogo para a tela Ao Vivo

**Arquivos:**
- Modificar: `dados/CatalogoDao.kt`
- Teste: `dados/CatalogoDaoTest.kt`

**Interfaces:**
- Produz:
  - `fun categorias(contaId: Long, tipo: TipoConteudo): Flow<List<Categoria>>`
  - `fun canais(contaId: Long): Flow<List<Canal>>` — todos os canais da conta (sem filtro;
    ~946 linhas no catálogo real, cabe inteiro em memória — diferente do anti-objetivo de
    filmes/séries, que são 10-20x maiores; o agrupamento/filtro por categoria/favoritos
    acontece em memória na `AoVivoViewModel`, T7).
  - `suspend fun programacao(contaId: Long, epgId: String, inicioMs: Long, fimMs: Long): List<Programa>`
    — janela de um dia, ordenada por `inicio`.

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `categorias filtra por tipo`()
@Test fun `canais lista todos os canais da conta`()
@Test fun `programacao retorna so os programas dentro da janela, ordenados`()
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar** as 3 queries (`SELECT` direto, sem lógica extra).
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: consultas de categoria/canais/programacao pra TV ao vivo`

