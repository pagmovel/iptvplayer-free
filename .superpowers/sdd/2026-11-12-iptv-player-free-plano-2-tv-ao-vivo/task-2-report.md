# T2: Favoritos — migração do Room + DAO — relatório

## O que foi implementado

- **`CanalFavorito`** (`app/src/main/java/.../dados/Entidades.kt`): nova `@Entity` com chave
  composta `["contaId", "nomeBase", "categoriaId"]`, `categoriaId: Long` (nunca `Long?`,
  conforme o brief — quem chama normaliza via `chaveCategoria`, de T1).
- **`CatalogoDao`** (`.../dados/CatalogoDao.kt`): `favoritar` (`@Insert(REPLACE)` — favoritar de
  novo o mesmo canal só atualiza `criadoEm`, não lança `SQLiteConstraintException`),
  `desfavoritar` (`DELETE` pela chave composta) e `favoritos(contaId): Flow<List<CanalFavorito>>`
  (mesmo padrão de `ContaDao.listar()`).
- **`BancoApp`** (`.../dados/BancoApp.kt`): `CanalFavorito::class` adicionada a `entities`,
  `version = 2`, e `companion object { val MIGRACAO_1_2 = object : Migration(1, 2) { ... } }`
  com SQL explícito (`CREATE TABLE IF NOT EXISTS CanalFavorito (...)`, idêntico byte a byte ao
  que o Room gerou em `2.json` — conferido manualmente) — migração real, nunca
  `fallbackToDestructiveMigration`.
- **`AppIptv.kt`**: `Room.databaseBuilder(...).addMigrations(BancoApp.MIGRACAO_1_2).build()` —
  sem isso a `Migration` existiria no código mas nunca seria usada pelo app real (o builder de
  produção é o único lugar que importa para "não apagar o banco do dono").
- **`app/schemas/.../2.json`**: gerado automaticamente pelo KSP/Room no build (não escrito à
  mão, confirmado após o build: `CREATE TABLE` da entidade bate exatamente com o SQL que escrevi
  na `Migration`).

## Desvio do brief: `MigrationTestHelper` não foi usado — achado real

O brief sugeria `androidx.room.testing.MigrationTestHelper` lendo o schema de
`app/schemas/`. Tentei essa rota (passos abaixo) e ela não funciona neste projeto
(unit test Robolectric, sem `androidTest`/emulador), por uma limitação real do AGP 9.5.1:

1. **Tentativa 1** (`room-testing` + `MigrationTestHelper` padrão): `FileNotFoundException:
   Cannot find the schema file in the assets folder`. `MigrationTestHelper` lê o schema via
   `AssetManager`/`InstrumentationRegistry`, e por padrão o Robolectric só expõe os assets do
   variant de produção (`android_merged_assets` → `mergeDebugAssets`), nunca o sourceSet `test`.
2. **Tentativa 2** (`testOptions.unitTests.isIncludeAndroidResources = true` +
   `sourceSets.test.assets.srcDirs("$projectDir/schemas")`): introduziu uma segunda falha —
   `IllegalAccessException: ... module java.base does not export jdk.internal.access` (o
   Robolectric passa a chamar `ApplicationSharedMemory.create()` ao montar a `Application` de
   teste, que precisa de um `--add-opens` do JDK 17) — corrigi com
   `it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")`, mas **a
   `FileNotFoundException` continuou**: verifiquei manualmente a pasta
   `app/build/intermediates/assets/debug/mergeDebugAssets` depois do build e ela não contém o
   schema — confirma que `sourceSets.test.assets` não é mesclado nos assets que o Robolectric
   enxerga nesta configuração (não existe task `mergeDebugUnitTestAssets` nem equivalente; só
   roda `mergeDebugAssets`, do variant de produção).

Decidi **não insistir** numa configuração de AGP específica de versão e sem documentação
confiável disponível (sem acesso à internet para confirmar a API exata da variant API para
isso), e usei uma abordagem mais robusta e sem essa dependência de infraestrutura:

- O teste (`MigracaoBancoTest.kt`) lê o `1.json` como **resource de teste comum**
  (`sourceSets.test.resources.srcDirs("$projectDir/schemas")` — classpath puro da JVM, sem
  `AssetManager`/Android nenhum) e recria o schema v1 com o **SQL exato** que está no JSON
  (`createSql` de cada entidade + `contentSyncTriggers` dos FTS + `setupQueries` do
  `room_master_table`, incluindo o `identityHash` da v1) — ou seja, o mesmo schema que o Room
  realmente gerou naquela versão, só que replay por SQL puro em vez de via `AssetManager`.
- Depois abre com o **`Room.databaseBuilder` real** (`Room.databaseBuilder(context,
  BancoApp::class.java, arquivo.absolutePath).addMigrations(BancoApp.MIGRACAO_1_2).build()`) —
  o mesmo builder de produção, com a mesma `Migration` registrada em `AppIptv.kt`. Isso na
  prática testa exatamente o cenário real: um banco de verdade na versão 1 sendo aberto por um
  `BancoApp` compilado na versão 2, passando pelo `onUpgrade`/`onValidateSchema` reais do Room
  (não um mock, não um atalho).

**Consequência:** removi a dependência `room-testing` que tinha adicionado (ponytail: código/dep
não usada não fica) — ela não é necessária para a abordagem final. Reverti também
`testOptions.unitTests.isIncludeAndroidResources` e o `add-opens`, que só existiam por causa da
tentativa com `MigrationTestHelper`. O `app/build.gradle.kts` ficou só com uma linha nova:
`sourceSets { getByName("test") { resources.srcDirs("$projectDir/schemas") } }`.

**Isto é uma preocupação a levantar para o controlador:** o brief pediu `MigrationTestHelper`
explicitamente como parte da interface "Produz". Se o controlador preferir investigar mais a
configuração do AGP 9.5.1 para fazer `MigrationTestHelper` funcionar de verdade (ex.: via
`androidTest` real num emulador, ou alguma API de variant ainda não tentada), é um esforço
adicional e separado — não bloqueante, porque o teste atual cobre o mesmo comportamento real
(dado sobrevive, tabela nova existe, SQL idêntico ao gerado pelo Room) usando o próprio
`Room.databaseBuilder` de produção.

## Evidência RED → GREEN

**RED** (implementação de produção guardada em `git stash`, só os testes novos presentes):
```
git stash push -m "RED temporario T2" -- app/src/main/java/.../AppIptv.kt \
    app/src/main/java/.../dados/BancoApp.kt app/src/main/java/.../dados/CatalogoDao.kt \
    app/src/main/java/.../dados/Entidades.kt

./scripts/gradle.ps1 :app:testDebugUnitTest --tests ...CatalogoDaoTest --tests ...MigracaoBancoTest
```
Saída: `compileDebugUnitTestKotlin FAILED`, com `Unresolved reference` em `CanalFavorito`,
`favoritar`, `favoritos` (`CatalogoDaoTest.kt:102,103,105,106`) e `BancoApp.MIGRACAO_1_2`
(`MigracaoBancoTest.kt:34`) — RED genuíno (não compila sem a implementação).

Em seguida `git stash pop` restaurou a implementação.

**GREEN** (depois de implementar + corrigir o `app/build.gradle.kts` — ver seção do desvio):
```
./scripts/gradle.ps1 :app:testDebugUnitTest --tests ...CatalogoDaoTest --tests ...MigracaoBancoTest
```
Saída: `BUILD SUCCESSFUL in 2m 58s`. Confirmado via XML de resultado:
- `MigracaoBancoTest`: `tests="1" failures="0" errors="0"`.
- `CatalogoDaoTest`: `tests="7" failures="0" errors="0"` (as 6 já existentes + a nova
  `favoritar e desfavoritar um canal agrupado`).

**Suíte completa** antes do commit:
```
./scripts/gradle.ps1 :app:testDebugUnitTest
```
Saída: `BUILD SUCCESSFUL in 1m 3s`. Soma de todos os `TEST-*.xml`: **53 testes, 0 falhas**.

## Arquivos alterados

- `app/src/main/java/io/github/pagmovel/iptvplayer/dados/Entidades.kt` — entidade `CanalFavorito`.
- `app/src/main/java/io/github/pagmovel/iptvplayer/dados/CatalogoDao.kt` — `favoritar`/`desfavoritar`/`favoritos`.
- `app/src/main/java/io/github/pagmovel/iptvplayer/dados/BancoApp.kt` — versão 2 + `MIGRACAO_1_2`.
- `app/src/main/java/io/github/pagmovel/iptvplayer/AppIptv.kt` — `.addMigrations(BancoApp.MIGRACAO_1_2)`.
- `app/build.gradle.kts` — `sourceSets.test.resources.srcDirs("$projectDir/schemas")` (ver desvio acima).
- `app/src/test/java/io/github/pagmovel/iptvplayer/dados/MigracaoBancoTest.kt` (novo).
- `app/src/test/java/io/github/pagmovel/iptvplayer/dados/CatalogoDaoTest.kt` — novo teste de favoritar/desfavoritar.
- `app/schemas/io.github.pagmovel.iptvplayer.dados.BancoApp/2.json` (gerado pelo build, não escrito à mão).
- `gradle/libs.versions.toml` — sem diff líquido (adicionei e depois removi `androidx-room-testing`).

## Achados da autorrevisão

- Migração é real: `Migration(1, 2)` com SQL explícito, registrada em `AppIptv.kt` (sem isso o
  app real nunca usaria a migração, só o código morto existiria).
- `categoriaId` em `CanalFavorito` é `Long`, nunca `Long?` — confirmado na entidade e em todo
  uso no DAO/teste.
- Teste de migração usa dados reais (SQL de verdade replayado do schema exportado, builder de
  produção real) — não é mock.
- `favoritar` usa `OnConflictStrategy.REPLACE` — decisão minha, não estava explícita no brief;
  justificativa: favoritar de novo o mesmo canal não deveria lançar excessão (mesmo padrão já
  usado em `inserirCanais`/`inserirFilmes`/`inserirSeries` deste DAO, inclusive com comentário
  documentando um achado real parecido).
- Removi a dependência `room-testing` que não ficou em uso, e as configurações de
  `testOptions`/`add-opens` que só existiam por causa da tentativa com `MigrationTestHelper` —
  não deixei configuração morta no `build.gradle.kts`.

## Dúvidas / preocupações para o controlador

1. O desvio do `MigrationTestHelper` (ver seção acima) é a principal pendência de decisão: a
   cobertura real está lá (schema recriado fielmente + builder de produção + `Migration` real),
   mas não é literalmente a ferramenta que o brief nomeou. Se isso for um problema, preciso de
   orientação sobre se vale investir em investigar a configuração de assets do AGP 9.5.1 mais a
   fundo (sem conexão à internet nesta sessão para confirmar a API certa) ou se a abordagem
   atual é aceitável.
2. Não toquei em `docs/superpowers/plans/2026-11-12-iptv-player-free-plano-2-tv-ao-vivo.md`
   (já aparecia modificado no `git status` antes de eu começar) nem no `.superpowers/sdd/.../`
   nem no `fwcd.kotlin-0.2.36.vsix` — não são meus, deixei de fora do commit.
