### T2: Favoritos — migração do Room + DAO

**Arquivos:**
- Modificar: `dados/Entidades.kt` (nova entidade), `dados/BancoApp.kt` (versão 2 + migração),
  `app/build.gradle.kts` (dependência `room-testing`), `gradle/libs.versions.toml`
- Teste: `dados/MigracaoBancoTest.kt` (novo), `dados/CatalogoDaoTest.kt` (casos novos)

**Interfaces:**
- Consome: chave de agrupamento de T1 (`chaveCategoria`).
- Produz:
  - `@Entity(primaryKeys = ["contaId", "nomeBase", "categoriaId"]) data class CanalFavorito(val contaId: Long, val nomeBase: String, val categoriaId: Long, val criadoEm: Long)`
    — `categoriaId` aqui **nunca** é `Long?`: quem chama já passa o valor normalizado por
    `chaveCategoria` (T1).
  - Em `CatalogoDao`: `suspend fun favoritar(favorito: CanalFavorito)`,
    `suspend fun desfavoritar(contaId: Long, nomeBase: String, categoriaId: Long)`,
    `fun favoritos(contaId: Long): Flow<List<CanalFavorito>>`.
  - `BancoApp` versão `2`, com `Migration(1, 2)` que só faz
    `CREATE TABLE IF NOT EXISTS CanalFavorito (...)` — não mexe nas tabelas existentes.

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
// MigracaoBancoTest.kt — usa MigrationTestHelper com o schema exportado em app/schemas/
@Test fun `migracao 1 para 2 cria a tabela CanalFavorito sem apagar dados existentes`()
// Abre um banco na versão 1 (schema exportado), insere uma Conta, roda a migração 1->2,
// confirma que a Conta continua lá e que um INSERT em CanalFavorito funciona.

// CatalogoDaoTest.kt
@Test fun `favoritar e desfavoritar um canal agrupado`()
// favoritar(...) -> favoritos(contaId) emite a lista com 1 item; desfavoritar(...) -> emite vazia.
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar.** Adicionar `androidx.room:room-testing` (versão `2.8.5`, mesma
  do catálogo já declarado) ao `libs.versions.toml` e como `testImplementation`. Escrever a
  `Migration(1, 2)` com o SQL exato da tabela (mesmas colunas da entidade, sem índice extra —
  a chave composta já serve). Rodar o app uma vez localmente **não apaga o banco do dono**
  porque é migração real, não `fallbackToDestructiveMigration`.
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: tabela de favoritos (migracao Room 1 -> 2)`

