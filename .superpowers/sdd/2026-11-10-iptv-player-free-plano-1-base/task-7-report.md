# Relatório — T7: Banco local (Room + FTS)

## Status: DONE

Commit: `6876c13` — `feat: banco local com busca FTS sem acento`.

## O que foi feito

Arquivos criados (pacote `io.github.pagmovel.iptvplayer.dados`):

- `app/src/main/java/.../dados/Entidades.kt` — `enum TipoConteudo` e as entidades
  `Categoria`, `Canal`, `Filme`, `Serie`, `Programa` (todas com chave primária composta
  incluindo `contaId`) e `Conta` (chave própria `id`, autoGenerate, usada pela T10).
- `app/src/main/java/.../dados/Fts.kt` — `CanalFts`, `FilmeFts`, `SerieFts`, cada uma
  `@Fts4(contentEntity = ..., tokenizer = FtsOptions.TOKENIZER_UNICODE61)` sobre o campo
  `nome` das entidades correspondentes.
- `app/src/main/java/.../dados/CatalogoDao.kt` — `CatalogoDao` (abstract class) e
  `data class Contagem`.
- `app/src/main/java/.../dados/BancoApp.kt` — `BancoApp : RoomDatabase()` (versão 1,
  `exportSchema = true`) e `ContaDao` (vazio, `@Dao abstract class`, preenchido na T10).
- `app/src/test/java/.../dados/CatalogoDaoTest.kt` — os 5 testes do brief, com Robolectric
  e banco em memória.
- `app/schemas/io.github.pagmovel.iptvplayer.dados.BancoApp/1.json` — esquema exportado
  pelo KSP, versionado no repositório.

Arquivos alterados:

- `gradle/libs.versions.toml` — versão `ksp = "2.3.13"` e alias de plugin `ksp`
  (`com.google.devtools.ksp`). Nenhuma versão existente foi alterada.
- `build.gradle.kts` (raiz) — `alias(libs.plugins.ksp) apply false`.
- `app/build.gradle.kts` — aplica o plugin `ksp`; bloco `ksp { arg("room.schemaLocation",
  "$projectDir/schemas") }`; dependências `androidx.room.runtime`/`androidx.room.ktx`
  (`implementation`) e `androidx.room.compiler` (`ksp(...)`); `robolectric` e
  `kotlinx.coroutines.test` em `testImplementation`.

## Decisões técnicas (o que exigiu verificação, não suposição)

- **Versão do plugin KSP**: desde a 2.3.0 o KSP desacoplou sua numeração da versão do
  Kotlin (antes era `<kotlin>-<ksp>`). Confirmei em `dl.google.com` e nos releases do
  GitHub `google/ksp` que `2.3.13` é a última estável, publicada em 2026-10-30 — mesma
  janela de tempo da escolha de `kotlin = "2.4.21"` no catálogo. O build real confirmou que
  funciona (KSP + Room processaram as entidades sem erro).
- **Room 2.8.5 ainda é `androidx.room` (não `androidx.room3`)**: a doc atual do Android
  (developer.android.com) já mostra exemplos com pacote `androidx.room3` — é da próxima
  versão major (Room 3.0, já publicada em paralelo). Baixei o `.aar`/`.jar` reais de
  `room-common:2.8.5` e `room-runtime-android:2.8.5` e confirmei pelas classes
  (`androidx/room/Fts4.class`, `androidx/room/FtsOptions.class`, etc.) que a 2.8.5 continua
  no pacote antigo. Por isso o código usa `androidx.room.Fts4`/`FtsOptions`, não `room3`.
- **FTS4 `contentEntity` não sincroniza automaticamente** ("external content table" do
  SQLite): confirmado em sqlite.org/fts3.html — o SQLite só sabe que o conteúdo mudou se
  alguém disser isso a ele. A solução adotada (e documentada em comentário no `Fts.kt`) é
  reconstruir o índice com `INSERT INTO <tabela>(<tabela>) VALUES ('rebuild')` depois de
  cada inserção/remoção em massa — é o mecanismo oficial do SQLite para isso, e se encaixa
  bem aqui porque a troca de catálogo já é um recarregamento completo (não incremental).
  `CatalogoDao` esconde esse detalhe: `inserirCanais`/`inserirFilmes`/`inserirSeries` e
  `apagarCatalogo` já chamam o rebuild internamente, dentro de `@Transaction`.
- **Pegadinha real encontrada rodando o teste**: a sintaxe do brief, `"palavra"*` (aspas
  fechando antes do asterisco), **não aplica prefixo** — o SQLite exige o asterisco
  **dentro** das aspas, `"palavra*"` (confirmado no próprio sqlite.org, exemplo oficial
  `"lin* app*"`). Com `"palavra"*` o teste `busca por prefixo enquanto digita` falhou (vi
  falhar de fato, não só proali). Troquei para `"palavra*"` e os 5 testes passaram. Deixei
  comentário no código citando a fonte, para não repetir o erro.
- **Busca por conta** (`buscarNomesCanais`): como a tabela FTS só guarda `nome` (não
  `contaId`), o filtro por conta exige voltar à tabela de origem — por isso o `JOIN
  CanalFts ON CanalFts.rowid = Canal.rowid` e o `WHERE Canal.contaId = :contaId` na mesma
  query. Isso é o "ligar por rowid" que o brief pede.
- **Robolectric**: usei `RuntimeEnvironment.getApplication()` (do próprio Robolectric) como
  `Context` do banco em memória, em vez de adicionar `androidx.test:core` só para
  `ApplicationProvider` — não precisava da dependência nova. `allowMainThreadQueries()`
  porque o teste roda no thread único do Robolectric.

## Testes

Comando: `.\scripts\gradle.ps1 testDebugUnitTest --tests '*CatalogoDaoTest'`

- 1ª rodada (Passo 2, sem as classes de produção): falhou por `Unresolved reference` em
  cada símbolo do pacote `dados` — confirma que os testes realmente testam algo.
- 2ª rodada (após implementar, com `"palavra"*`): 4 de 5 passaram; `busca por prefixo
  enquanto digita` falhou (`AssertionError`) — a pegadinha do asterisco acima.
- 3ª rodada (após corrigir para `"palavra*"`): **BUILD SUCCESSFUL, 5/5 testes passando.**
- Também roda a suíte completa do módulo (`testDebugUnitTest`, sem filtro) para garantir
  que `EsqueletoTest` e `LinkXtreamTest` (da T1/T4) não foram afetados pelas dependências
  novas — resultado anotado no contrato final da entrega.

## Autorrevisão

- **Completude**: os 4 arquivos de produção e o teste batem exatamente com as interfaces do
  brief (nomes de classe, de campo e de método). `ContaDao` e `fun contaDao()` existem
  (vazios, como o brief pede — "preenchido na T10") sem criar um arquivo novo fora da lista.
- **YAGNI/ponytail**: não criei paginação/limite em `buscarNomesCanais` (não pedido agora —
  "a busca completa vem no Plano 3"); não criei índice extra para `Programa` (a chave
  composta já serve de índice); não adicionei `androidx.test:core` só por
  `ApplicationProvider` quando `RuntimeEnvironment.getApplication()` já resolve. Marquei com
  `// ponytail:` a única simplificação deliberada (rebuild total do FTS a cada carga, não
  incremental).
- **Testes**: os dois mais sensíveis segundo o brief — acento/maiúscula e contas não se
  misturarem — passam porque (a) `TOKENIZER_UNICODE61` já faz casefold e remove diacrítico
  por padrão, sem precisar de `tokenizerArgs`, e (b) o filtro por `contaId` está na própria
  query (`WHERE Canal.contaId = :contaId`), não em filtro posterior em Kotlin — ou seja, não
  existe caminho para misturar contas mesmo que o termo de busca combine com as duas.
- **Nada de lista inteira em memória**: todas as consultas (`contagem`, `buscarNomesCanais`)
  resolvem no SQL (COUNT, MATCH+JOIN); não há `SELECT *` seguido de filtro em Kotlin.

## Correção pós-revisão (commit `9862d02`)

A revisão apontou um Achado Importante: o comentário/código partiam da premissa de que o
SQLite não sincroniza sozinho uma tabela FTS4 "external content" (`contentEntity`), e por
isso `CatalogoDao` chamava `INSERT INTO <tabela>(<tabela>) VALUES ('rebuild')` depois de
cada inserção/remoção em massa. Essa premissa está certa para SQLite puro (é o que eu
tinha verificado em sqlite.org/fts3.html antes de implementar), mas o **Room** adiciona uma
camada de sincronia automática que eu não tinha verificado: ele gera triggers de conteúdo
(`room_fts_content_sync_<Tabela>Fts_BEFORE/AFTER_INSERT/UPDATE/DELETE`) sempre que a
entidade FTS usa `contentEntity`.

Verifiquei isso de forma independente antes de aplicar a correção — não só confiei no
relato da revisão — abrindo o próprio `app/schemas/.../1.json` que o KSP gerou nesta
tarefa: a seção `contentSyncTriggers` de `CanalFts`/`FilmeFts`/`SerieFts` lista os 4
triggers (`BEFORE_UPDATE`, `BEFORE_DELETE`, `AFTER_UPDATE`, `AFTER_INSERT`) que fazem
`DELETE`/`INSERT` em `CanalFts`/`FilmeFts`/`SerieFts` a cada mudança em `Canal`/`Filme`/
`Serie`. Ou seja, a sincronia já acontecia nos próprios testes anteriores — o `rebuild`
manual era redundante, e caro: reconstrói o índice inteiro (de todas as contas) a cada
chamada, custo que se repetiria sempre que a T9 chamar este DAO dentro de
`withTransaction`.

**O que mudou:**
- `CatalogoDao.kt`: removidos `reconstruirIndiceCanais/Filmes/Series()` e os métodos
  `*Bruto` que existiam só para encaixar o rebuild depois do insert. `inserirCanais`,
  `inserirFilmes` e `inserirSeries` voltaram a ser `@Insert abstract suspend fun` diretos
  (sem `@Transaction` — não precisam mais, um único `@Insert` já é atômico). `apagarCatalogo`
  manteve o `@Transaction` (ainda são 4 `DELETE`s que precisam ser atômicos entre si), só
  perdeu as 3 chamadas de rebuild no final.
- `Fts.kt`: comentário corrigido — explica que o Room sincroniza via triggers
  automaticamente (citando o schema exportado como evidência), em vez de dizer que é
  preciso reconstruir manualmente.
- Achados menores do revisor: o item 1 (rebuild com lista vazia) desapareceu junto com a
  remoção do rebuild, não precisou de tratamento separado. O item 2 (escape de aspas em
  `termoParaFts`) foi mantido como está, por indicação do próprio revisor.

**Teste:** `./scripts/gradle.ps1 testDebugUnitTest --tests '*CatalogoDaoTest'` →
`BUILD SUCCESSFUL`, 5/5 passando (inclusive o de acento/maiúscula e o de contas não se
misturarem) usando só os triggers automáticos do Room, sem nenhuma chamada manual de
rebuild.
