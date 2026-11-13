# IPTV Player Free — Plano 2: TV ao vivo

> **Para agentes:** SUB-SKILL OBRIGATÓRIA: use superpowers:subagent-driven-development
> (recomendado) ou superpowers:executing-plans para executar este plano tarefa por tarefa. Os
> passos usam checkbox (`- [ ]`).
>
> **Para TODO subagente despachado na execução deste plano:** leia e use os tools do plugin
> context-mode (`ctx_execute`, `ctx_execute_file`, `ctx_batch_execute`, `ctx_search`) para ler
> arquivos, rodar comandos (build/teste) e buscar no código, em vez de `Read`/`Bash`/`Grep`
> diretos. `Read` nativo só no arquivo que vai receber `Edit` na sequência imediata. Imprima só
> o que responde à pergunta (ex.: resultado de suíte de teste → só a linha de resumo e as
> falhas), nunca a saída inteira de um comando.
>
> **Autorizado pelo dono em 2026-11-12**, depois de brainstorming e builder-check (ver
> `docs/superpowers/specs/2026-11-10-iptv-player-free-design.md`, seção "TV ao vivo").

**Objetivo:** tela de TV ao vivo com player (Media3/ExoPlayer), lista de canais por
categoria/favoritos, troca de qualidade (SD/HD/FHD/4K) automática e manual, aba de
programação (Ontem/Hoje/Amanhã), tela cheia ao deitar o celular, e uma tela de Configurações
nova — tudo contra o catálogo real já carregado pelo Plano 1.

**Arquitetura:** pacote novo `aovivo/` para o agrupamento de qualidade e a máquina de estados
da troca de qualidade (lógica pura, sem Android, testável com JUnit puro) e `PlayerAoVivo.kt`
(a parte fina que liga isso ao ExoPlayer de verdade). Pacote novo `configuracoes/` para a
única preferência desta fase (tempo de buffer), guardada em `SharedPreferences` — não entra no
Room porque não é dado de catálogo nem precisa de migração. `dados/` ganha a tabela de
favoritos (migração do Room, versão 1 → 2) e as consultas que a tela Ao Vivo precisa. UI nova
em `ui/aovivo/` e `ui/configuracoes/`.

**Tecnologia:** adiciona `androidx.media3:media3-exoplayer` e `androidx.media3:media3-ui`
(versão **1.11.1** — última estável sem alpha/beta/rc em
`dl.google.com/android/maven2/androidx/media3/media3-exoplayer/maven-metadata.xml`,
consultado em 2026-11-12, mesma convenção do `gradle/libs.versions.toml`) e
`androidx.room:room-testing` (mesma versão do Room já usado, **2.8.5**, para o teste de
migração). Resto igual ao Plano 1: Kotlin, Compose (Material 3), Room, kotlinx-coroutines-test,
Robolectric.

**Design:** [docs/superpowers/specs/2026-11-10-iptv-player-free-design.md](../specs/2026-11-10-iptv-player-free-design.md),
seção "TV ao vivo" (dentro de "## 4. Decisões já tomadas") e os itens relevantes de
"## 5. Deixado para quem for construir". Leia antes de começar.

## Global Constraints

- Conta do provedor permite só **2 conexões simultâneas** (design §3.1) — nenhuma lógica de
  retry pode tentar sem limite.
- Tempo de buffer/erro antes de trocar de qualidade: **padrão 8 segundos**, configurável.
  **O timer reinicia a cada qualidade tentada** (não é orçamento único para a cascata).
- Um **erro definitivo** do player (HTTP não-2xx, stream não encontrado, codec não suportado)
  troca de qualidade **na hora**, sem esperar o timer de buffer.
- Se a qualidade mais baixa disponível também falhar: **mostra erro e para**, com botão de
  tentar de novo manualmente — nunca insiste sozinho sem parar.
- Agrupamento de qualidade: chave é **nome-sem-sufixo + `categoryId`** (não só o nome).
- Troca manual não desliga a automática — só muda o ponto de partida da cascata.
- `epg_channel_id` **é o mesmo** entre as variantes de qualidade do mesmo canal (confirmado,
  skill `iptv-xtream-carga` item 4) — qualquer variante serve para buscar a programação.
- Segundo plano (app minimizado, tela bloqueada, sai da TV ao vivo): o vídeo **para
  completamente** nesta fase — sem serviço em primeiro plano nem notificação.
- `android:configChanges="orientation|screenSize|screenLayout"` na `MainActivity` *(decisão
  minha)*: sem isso, o Android recria a Activity inteira ao girar o celular — e com ela, todo
  o estado do Compose e o player (que não é um `androidx.lifecycle.ViewModel`, igual ao resto
  do app). Reabrir o vídeo do zero a cada giro contradiz "ao deitar, tela cheia" ser fluido.
- Testes versionados usam só dados sintéticos — nunca os reais de `temp/amostras/`.
- pt-BR em toda mensagem de usuário. Tema escuro Material 3 já existe em `ui/Tema.kt`
  (fundo `#0f1115`, destaque `#4fc3f7`) — reaproveitar, não recriar.

## Review Focus

- Canal com **só 1 variante de qualidade** (muito comum nos dados reais — ex. "AGRO BRASIL
  HD" sozinho): a cascata não pode tentar "descer" para nada; uma falha nela vai direto pro
  erro terminal (T5).
- Canal **sem nenhuma entrada de programação** (`get_short_epg`/XMLTV vazio pro `epgId`,
  achado real documentado na skill item 5): a aba Programação mostra "Sem programação", nunca
  lista vazia sem explicação nem trava (T8).
- **Canal sem sufixo de qualidade reconhecido no nome** (defensivo — os dados reais vistos até
  agora sempre têm sufixo, mas o algoritmo não pode supor isso para sempre): vira seu próprio
  grupo de 1 variante só, não quebra o agrupamento dos outros (T1).
- **Dois canais com o mesmo nome-base em categorias diferentes** não podem se misturar num
  canal agrupado só — é exatamente o que a chave nome+categoria evita (T1).
- **`categoriaId` nulo** (`Canal.categoriaId` é `Long?`) usado como parte da chave de
  agrupamento/favoritos não pode quebrar a unicidade (`NULL` não colide com `NULL` em chave
  composta do SQLite) — normalizado para um valor sentinela não-nulo (T1, T2).

---

## Acompanhamento das tarefas

Estados: `pendente` → `concluída` (código pronto) → `testada` (verificação rodou) → `validada`
(dono confirmou no celular). Atualize esta tabela a cada avanço.

| Tarefa | Descrição | Estado |
|---|---|---|
| T1 | Agrupamento de canais por qualidade (SD/HD/FHD/4K) | `testada` |
| T2 | Favoritos: migração do Room + DAO | `testada` |
| T3 | Consultas do catálogo para a tela Ao Vivo (categorias, canais, programação) | `testada` |
| T4 | Configurações: tempo de buffer | `testada` |
| T5 | Cascata de qualidade (máquina de estados pura) | `testada` |
| T6 | Player Media3/ExoPlayer | `testada` |
| T7 | Tela Ao Vivo — aba Canais | `testada` |
| T8 | Tela Ao Vivo — aba Programação | `testada` |
| T9 | OSD do player | `testada` |
| T10 | Tela cheia ao deitar + navegação + Release v0.2.0 | `testada` (revisão final + fix wave completos; tag v0.2.0 pendente) |
| T11 | Validação do dono no celular, contra o provedor real | `pendente` |

---

## Fase 2 · TV ao vivo

Base dos pacotes novos: `app/src/main/java/io/github/pagmovel/iptvplayer/{aovivo,configuracoes}/`
e `app/src/main/java/io/github/pagmovel/iptvplayer/ui/{aovivo,configuracoes}/`. Testes em
`app/src/test/java/io/github/pagmovel/iptvplayer/<mesmo-pacote>/`. Verificação:
`./scripts/gradle.ps1 testDebugUnitTest --tests '<Classe>'`. Esperado: `BUILD SUCCESSFUL`, ou a
falha do teste novo no passo "ver falhar".

### T1: Agrupamento de canais por qualidade (SD/HD/FHD/4K)

**Arquivos:**
- Criar: `aovivo/CanalAgrupado.kt`
- Teste: `aovivo/CanalAgrupadoTest.kt`

**Interfaces:**
- Consome: `Canal` (`dados/Entidades.kt`, já existe: `contaId`, `streamId`, `nome`,
  `categoriaId: Long?`, `icone`, `epgId`, `numero`).
- Produz:
  - `enum class Qualidade(val rotulo: String) { SD("SD"), HD("HD"), FHD("FHD"), QUATRO_K("4K") }`
    — ordem de declaração é da pior pra melhor (`Qualidade.entries` ordenado assim).
  - `fun chaveCategoria(categoriaId: Long?): Long` — normaliza `null` para `-1L` (Review
    Focus: chave de agrupamento nunca usa `Long?` cru).
  - `data class VarianteQualidade(val qualidade: Qualidade, val streamId: Long)`
  - `data class CanalAgrupado(val nomeBase: String, val categoriaId: Long, val icone: String?, val epgId: String?, val numero: Int?, val variantes: List<VarianteQualidade>)`
    — `variantes` sempre ordenada da **maior pra menor** qualidade.
  - `fun qualidadeDoNome(nome: String): Qualidade?`
  - `fun nomeBase(nome: String): String`
  - `fun agruparPorQualidade(canais: List<Canal>): List<CanalAgrupado>`

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `agrupa variantes do mesmo canal pelo nome-sem-sufixo e categoria`()
// 3 Canal sintéticos: "CANAL TESTE FHD", "CANAL TESTE HD", "CANAL TESTE SD", mesma categoriaId.
// agruparPorQualidade(...) -> 1 CanalAgrupado, nomeBase == "CANAL TESTE",
// variantes == [FHD, HD, SD] nessa ordem (maior pra menor).

@Test fun `canais com nome-base igual em categorias diferentes nao se misturam`()
// "CANAL X HD" categoria 1, "CANAL X HD" categoria 2 -> 2 CanalAgrupado distintos.

@Test fun `categoriaId nulo normaliza para -1 sem colidir com categoria real -1`()
// "CANAL Y HD" categoriaId = null e "CANAL Y HD" categoriaId = -1L (hipotético) ->
// terminam na mesma chave normalizada -1L -> 1 CanalAgrupado só (comportamento esperado e
// documentado, já que -1L é o sentinela).

@Test fun `canal sem sufixo de qualidade reconhecido vira grupo de 1 variante`()
// "CANAL SEM SUFIXO" (nenhum SD/HD/FHD/4K no fim) -> qualidadeDoNome(...) == null;
// agruparPorQualidade ainda produz 1 CanalAgrupado com nomeBase == nome inteiro e
// 1 variante (decisão: variante sem qualidade reconhecida entra como Qualidade.SD, pior
// caso, nunca quebra a função).

@Test fun `qualidadeDoNome reconhece os 4 sufixos, case-insensitive`()
// "X SD", "x hd", "X Fhd", "X 4K" -> SD, HD, FHD, QUATRO_K.
```

- [ ] **Passo 2: Rodar e ver falhar.** `./scripts/gradle.ps1 testDebugUnitTest --tests '*CanalAgrupadoTest'`
- [ ] **Passo 3: Implementar.** Regex para o sufixo:
  `Regex("""(?i)\s+(4K|FHD|HD|SD)$""")` — casa só o último token, sem ambiguidade entre "HD"
  e "FHD" porque a alternância precisa bater com o token inteiro até o fim da string.
  `agruparPorQualidade` agrupa por `Pair(nomeBase(c.nome), chaveCategoria(c.categoriaId))`,
  usa `icone`/`epgId`/`numero` de qualquer uma das variantes do grupo (são do mesmo canal
  lógico), e ordena `variantes` por `qualidade.ordinal` decrescente.
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: agrupa canais por qualidade SD/HD/FHD/4K`

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

### T4: Configurações — tempo de buffer

**Arquivos:**
- Criar: `configuracoes/ConfiguracoesRepositorio.kt`
- Teste: `configuracoes/ConfiguracoesRepositorioTest.kt`

**Interfaces:**
- Produz: `class ConfiguracoesRepositorio(private val prefs: SharedPreferences)` com
  `fun tempoBufferMs(): Long` (padrão **8000L**) e `fun definirTempoBufferSegundos(segundos: Int)`.
  *(Decisão minha: `SharedPreferences`, não Room — é 1 valor só, sem necessidade de migração
  nem de consulta reativa entre telas; `AppIptv` ganha `val configuracoes: ConfiguracoesRepositorio by lazy { ConfiguracoesRepositorio(getSharedPreferences("config", MODE_PRIVATE)) }`.)*

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `tempoBufferMs comeca em 8000 (8 segundos) por padrao`()
@Test fun `definirTempoBufferSegundos muda o valor lido depois`()
```

- [ ] **Passo 2: Rodar e ver falhar.** (Robolectric: `SharedPreferences` real do Android, roda
  igual aos outros testes deste projeto.)
- [ ] **Passo 3: Implementar.**
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: configuracao do tempo de buffer antes da troca de qualidade`

### T5: Cascata de qualidade (máquina de estados pura)

Núcleo da troca automática/manual — **sem Android, sem ExoPlayer**, só lógica e
`kotlinx-coroutines-test` para o tempo, testável rápido e sem Robolectric.

**Arquivos:**
- Criar: `aovivo/CascataQualidade.kt`
- Teste: `aovivo/CascataQualidadeTest.kt`

**Interfaces:**
- Consome: `CanalAgrupado`, `VarianteQualidade`, `Qualidade` (T1); `tempoBufferMs` (T4, passado
  por parâmetro — esta classe não lê `ConfiguracoesRepositorio` diretamente, pra não depender
  de Android nos testes).
- Produz:
  - `sealed class EventoCascata { data class Tocando(val variante: VarianteQualidade): EventoCascata(); data class Trocou(val de: Qualidade, val para: Qualidade): EventoCascata(); object FalhaTerminal: EventoCascata() }`
  - `class CascataQualidade(private val canal: CanalAgrupado, private val tempoBufferMs: Long, private val escopo: CoroutineScope)` com:
    - `val eventos: Flow<EventoCascata>`
    - `fun iniciar()` — começa pela variante de maior qualidade, emite `Tocando`.
    - `fun aoTravar()` — chamado quando o player entra em buffering; inicia (ou reinicia, se
      já ativo) o timer de `tempoBufferMs`; se o timer expirar sem `aoEstabilizar()`, desce
      uma qualidade (ou `FalhaTerminal` se já estava na pior) e emite `Trocou`/`FalhaTerminal`.
    - `fun aoEstabilizar()` — cancela o timer pendente (reproduzindo bem).
    - `fun aoErroDefinitivo()` — desce uma qualidade **na hora**, sem esperar timer (ou
      `FalhaTerminal` se já estava na pior).
    - `fun escolherManualmente(qualidade: Qualidade)` — muda a variante atual sem mexer no
      timer; a cascata automática continua valendo a partir daí.
    - `fun tentarNovamente()` — só válido depois de `FalhaTerminal`; reinicia da maior
      qualidade de novo.

- [ ] **Passo 1: Escrever os testes que falham** (usar `runTest` do `kotlinx-coroutines-test`
  para controlar o tempo sem esperar de verdade):

```kotlin
@Test fun `inicia sempre na maior qualidade disponivel`()
// CanalAgrupado com [FHD, HD, SD] -> iniciar() emite Tocando(FHD).

@Test fun `trava por mais que o tempo configurado desce uma qualidade e reinicia o timer`()
// iniciar() (FHD); aoTravar(); advanceTimeBy(tempoBufferMs); emite Trocou(FHD, HD).
// aoTravar() de novo; advanceTimeBy(tempoBufferMs/2); nao emite nada ainda (timer reiniciou).

@Test fun `travar por menos que o tempo configurado nao troca`()
// aoTravar(); advanceTimeBy(tempoBufferMs - 1); aoEstabilizar(); nenhum Trocou emitido.

@Test fun `erro definitivo troca na hora, sem esperar o timer`()
// aoErroDefinitivo() emite Trocou(...) imediatamente, sem advanceTimeBy.

@Test fun `canal com 1 variante so vai direto pra falha terminal ao travar`()
// CanalAgrupado com 1 variante só; aoTravar() + tempo todo -> emite FalhaTerminal, nao Trocou.

@Test fun `se a pior qualidade tambem falhar, emite falha terminal e para`()
// chega no SD (pior), trava de novo -> FalhaTerminal; nenhum evento Trocou depois disso
// sem um tentarNovamente() explicito.

@Test fun `escolha manual muda a variante sem desligar a cascata automatica`()
// iniciar() (FHD); escolherManualmente(SD); aoTravar() + tempo -> FalhaTerminal (SD era a
// pior) -- prova que a automatica continua agindo a partir da escolha manual.

@Test fun `tentarNovamente apos falha terminal reinicia da maior qualidade`()
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar** com `kotlinx.coroutines.Job`/`delay(tempoBufferMs)` lançado em
  `escopo`; `aoTravar()` cancela o job anterior antes de lançar um novo (isso é o "reinicia o
  timer"); `aoEstabilizar()` só cancela, sem lançar nada.
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: maquina de estados da troca automatica/manual de qualidade`

### T6: Player Media3/ExoPlayer

A parte fina que liga `CascataQualidade` (T5) a um `ExoPlayer` de verdade. Não tem suíte
unitária própria de peso (ExoPlayer real não roda bem em JVM puro nem vale a pena fingir) —
validação principal é manual, no celular (T11).

**Arquivos:**
- Criar: `aovivo/PlayerAoVivo.kt`
- Modificar: `gradle/libs.versions.toml`, `app/build.gradle.kts` (dependências Media3),
  `app/src/main/AndroidManifest.xml` (`android:configChanges` na `MainActivity`)

**Interfaces:**
- Consome: `CascataQualidade` (T5), `XtreamApi.urlAoVivo(streamId): String` (já existe, Plano 1).
- Produz: `class PlayerAoVivo(context: Context, private val montarUrl: (Long) -> String)` com
  `val exoPlayer: ExoPlayer` (exposto para o `AndroidView`/`PlayerView` da UI, T7),
  `fun abrir(canal: CanalAgrupado, cascata: CascataQualidade)` (liga os callbacks do
  `Player.Listener` a `cascata.aoTravar()`/`aoEstabilizar()`/`aoErroDefinitivo()`, e observa
  `cascata.eventos` para trocar o `MediaItem` da variante atual), `fun liberar()`.

- [ ] **Passo 1: Adicionar as dependências.** `media3-exoplayer` e `media3-ui`, versão
  `1.11.1`, no `libs.versions.toml` (seguindo o comentário de fonte já usado nas outras
  versões) e em `dependencies { implementation(...) }` no `app/build.gradle.kts`.
- [ ] **Passo 2: Implementar `PlayerAoVivo`.** `Player.Listener.onPlaybackStateChanged`:
  `STATE_BUFFERING` → `cascata.aoTravar()`; `STATE_READY` → `cascata.aoEstabilizar()`.
  `Player.Listener.onPlayerError` → `cascata.aoErroDefinitivo()`. Ao coletar um evento
  `Trocou`/`Tocando` do `cascata.eventos`, troca o `MediaItem` para
  `montarUrl(variante.streamId)` e chama `exoPlayer.setMediaItem(...)` +
  `prepare()` + `play()`.
- [ ] **Passo 3: `android:configChanges="orientation|screenSize|screenLayout"` na
  `MainActivity`** (ver Global Constraints).
- [ ] **Passo 4: Rodar `./scripts/gradle.ps1 assembleDebug`.** Esperado: `BUILD SUCCESSFUL`
  (prova que as dependências novas resolvem e o projeto inteiro ainda compila).
- [ ] **Passo 5: Commit.** `feat: player Media3 ligado a cascata de qualidade`

### T7: Tela Ao Vivo — aba Canais

**Arquivos:**
- Criar: `ui/aovivo/AoVivoViewModel.kt`, `ui/aovivo/AoVivoTela.kt`, `ui/aovivo/CanaisAba.kt`
- Modificar: `ui/Navegacao.kt`, `ui/resumo/ResumoTela.kt` (botão "Assistir ao vivo")
- Teste: nenhum automatizado de Compose nesta fase (mesmo padrão do Plano 1 — UI valida no
  celular, T11); `AoVivoViewModel` tem a parte não-Compose testável se tiver lógica própria
  além de repassar chamadas (decisão de quem implementar: só escrever teste se sobrar lógica
  não-trivial fora do que T1/T3/T5 já cobrem).

**Interfaces:**
- Consome: `CatalogoDao.canais/categorias/favoritos` (T3), `agruparPorQualidade` (T1),
  `CanalFavorito` (T2), `ContasRepositorio.credenciais`/`contaAtiva` (Plano 1),
  `CascataQualidade`/`EventoCascata` (T5), `PlayerAoVivo` (T6),
  `ConfiguracoesRepositorio.tempoBufferMs()` (T4, lido a cada `selecionarCanal` — uma troca
  feita em Configurações só vale a partir do próximo canal aberto, não precisa recriar a
  cascata do canal já tocando).
- Produz: `AoVivoViewModel` expõe os canais agrupados já filtrados por categoria/favoritos
  selecionados, o estado atual do player (`Tocando`/`Trocou`/`FalhaTerminal`, via
  `cascata.eventos`), e as ações `selecionarCanal(CanalAgrupado)` (cria uma `CascataQualidade`
  nova pro canal e chama `playerAoVivo.abrir(canal, cascata)`), `favoritar`/`desfavoritar`,
  `filtrarPorCategoria(categoriaId: Long?)` (`null` = Todos), `filtrarFavoritos(Boolean)`,
  `tentarNovamente()` (repassa pra `cascata.tentarNovamente()`), `pausar()`/`retomar()`
  (chamados pelo ciclo de vida da tela, passo 4 abaixo).

- [ ] **Passo 1: Implementar `AoVivoViewModel`** combinando `canais()` + `categorias()` +
  `favoritos()` (T3) com `agruparPorQualidade` (T1) e o filtro ativo, igual ao padrão de
  `ResumoViewModel`/`ContasViewModel` (classe comum, sem `androidx.lifecycle.ViewModel`).
  `selecionarCanal` guarda a `CascataQualidade` ativa (uma por vez — trocar de canal cancela
  a anterior) e liga `playerAoVivo.abrir(canal, cascata)`.
- [ ] **Passo 2: Implementar `CanaisAba`** — chips de categoria (incluindo "Favoritos" e
  "Todos", design §4), lista de `CanalAgrupado` com nome, ícone (ou nome sobre fundo neutro,
  mesma regra do design §4 "Navegação"), estrela de favorito, e o programa atual do canal
  **só se** a janela de hoje já tiver sido carregada (reaproveita `programacao`, T3, com a
  janela do instante atual) — "Sem programação" se vazio, nunca espaço em branco.
- [ ] **Passo 3: Implementar `AoVivoTela`** — hospeda o `PlayerAoVivo` (T6) via `AndroidView`
  wrapping `PlayerView`, a `CanaisAba` abaixo (parte da tela dividida), e um `TabRow` simples
  pra alternar com a aba Programação (T8). Quando o estado do player for `FalhaTerminal`
  (Global Constraints — qualidade mais baixa também falhou), mostra uma mensagem de erro por
  cima do vídeo com um botão "Tentar de novo" chamando `viewModel.tentarNovamente()` — nunca
  tenta sozinho sem esse toque.
- [ ] **Passo 4: Parar o vídeo em segundo plano** (Global Constraints). `AoVivoTela` observa o
  ciclo de vida (`LifecycleEventObserver`/`DisposableEffect` com `LocalLifecycleOwner`):
  `ON_STOP` chama `viewModel.pausar()` (`exoPlayer.pause()` dentro de `PlayerAoVivo`, sem
  liberar o player); `ON_START` chama `viewModel.retomar()` (volta a tocar o canal atual do
  zero — reabrir, não é retomar posição, já que é transmissão ao vivo). `ON_DISPOSE` libera o
  player de verdade (`PlayerAoVivo.liberar()`, T6) pra não vazar o `ExoPlayer` ao sair da tela.
- [ ] **Passo 5: Ligar a navegação.** `Navegacao.kt` ganha `Tela.AO_VIVO`; `ResumoTela` ganha
  o botão "Assistir ao vivo" ao lado de "Gerenciar contas" *(decisão minha: sem o bottom bar
  completo de 4 abas do design ainda — Filmes/Séries não existem até o Plano 3; construir a
  barra inteira agora deixaria 2 abas mortas. A barra de verdade entra no plano que criar a
  última das 4 telas)*.
- [ ] **Passo 6: Rodar `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`.** Esperado:
  `BUILD SUCCESSFUL`.
- [ ] **Passo 7: Commit.** `feat: tela de TV ao vivo -- aba Canais`

### T8: Tela Ao Vivo — aba Programação

**Arquivos:**
- Criar: `ui/aovivo/ProgramacaoAba.kt`
- Modificar: `ui/aovivo/AoVivoViewModel.kt`, `ui/aovivo/AoVivoTela.kt`

**Interfaces:**
- Consome: `CatalogoDao.programacao` (T3), `epgId` do `CanalAgrupado` atual (T1).
- Produz: `AoVivoViewModel.programacaoDoDia(dia: DiaPrograma): List<Programa>` com
  `enum class DiaPrograma { ONTEM, HOJE, AMANHA }`.

- [ ] **Passo 1: Implementar o cálculo da janela do dia** (início/fim em epoch ms a partir de
  `DiaPrograma` + fuso do aparelho) **em `AoVivoViewModel`**, chamando `programacao` (T3).
- [ ] **Passo 2: Implementar `ProgramacaoAba`** — seletor Ontem/Hoje/Amanhã (design §4), lista
  de programas com horário, e **"Sem programação"** quando a lista vier vazia (Review Focus) —
  seja porque o canal não tem `epgId`, seja porque a janela não tem nenhum programa.
- [ ] **Passo 3: Rodar `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`.**
- [ ] **Passo 4: Commit.** `feat: tela de TV ao vivo -- aba Programacao`

### T9: OSD do player

**Arquivos:**
- Criar: `ui/aovivo/Osd.kt`
- Modificar: `ui/aovivo/AoVivoTela.kt`, `ui/aovivo/AoVivoViewModel.kt`

**Interfaces:**
- Consome: `CascataQualidade.eventos` (T5, pro selo de troca de qualidade), navegação para
  Configurações/Gerenciar contas (T4, Plano 1).
- Produz: `Osd` composable recebendo `visivel: Boolean`, `aoTocarNaTela: () -> Unit` (reseta o
  timer de auto-esconder), e as ações (voltar, trocar qualidade manualmente, canal
  anterior/próximo, abrir grade de programação como overlay, Gerenciar contas, Configurações).

- [ ] **Passo 1: Implementar o estado de visibilidade** em `AoVivoTela` — `mutableStateOf(true)`
  que vira `false` depois de alguns segundos sem toque (`LaunchedEffect` com `delay`,
  reiniciado a cada toque), sobre um `Box` que detecta toque no vídeo.
- [ ] **Passo 2: Implementar `Osd`** — barra com voltar, nome do canal + programa atual
  (reaproveita a mesma busca de "programa atual" da `CanaisAba`, T7), seletor de qualidade
  (lista só as `variantes` do `CanalAgrupado` atual, T1), canal anterior/próximo (navega
  dentro da lista filtrada ativa no momento, T7 — *decisão minha*, ver design), atalho pra
  grade de programação (abre `ProgramacaoAba`, T8, como overlay por cima do vídeo em tela
  cheia), e atalhos pra Gerenciar contas / Configurações. Selo rápido de "Mudou para X" ao
  receber um evento `Trocou` de `CascataQualidade` (também um `LaunchedEffect` com `delay`
  pra sumir sozinho).
- [ ] **Passo 3: Rodar `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`.**
- [ ] **Passo 4: Commit.** `feat: OSD do player de TV ao vivo`

### T10: Tela cheia ao deitar + navegação + Release v0.2.0

**Arquivos:**
- Modificar: `ui/aovivo/AoVivoTela.kt`
- Criar: `ui/configuracoes/ConfiguracoesViewModel.kt`, `ui/configuracoes/ConfiguracoesTela.kt`
- Modificar: `ui/Navegacao.kt`, `.claude/skills/iptv-xtream-carga/SKILL.md` ou nova skill
  `iptv-tv-ao-vivo` (documentar a cascata de qualidade e os achados desta fase)

**Interfaces:**
- Consome: `ConfiguracoesRepositorio` (T4).
- Produz: `Tela.CONFIGURACOES` em `Navegacao.kt`; `ConfiguracoesTela` com o campo do tempo de
  buffer (segundos, padrão 8).

- [ ] **Passo 1: Detectar orientação em `AoVivoTela`** via
  `LocalConfiguration.current.orientation` — `ORIENTATION_LANDSCAPE` mostra só o vídeo em
  tela cheia (sem as abas Canais/Programação, que ficam atrás do OSD, T9, como overlay);
  `ORIENTATION_PORTRAIT` volta pra tela dividida.
- [ ] **Passo 2: Implementar `ConfiguracoesTela`/`ConfiguracoesViewModel`** — único campo:
  tempo de buffer em segundos, lendo/gravando em `ConfiguracoesRepositorio` (T4). Acessível
  pelo atalho do OSD (T9) e de onde "Gerenciar contas" já é hoje (`ResumoTela`, Plano 1).
- [ ] **Passo 3: Rodar `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`.** Esperado:
  `BUILD SUCCESSFUL`.
- [ ] **Passo 4: Commit.** `feat: tela cheia ao deitar, tela de Configuracoes e navegacao`
- [ ] **Passo 5: Documentar.** Nova skill `.claude/skills/iptv-tv-ao-vivo/SKILL.md` com: o
  algoritmo de agrupamento por sufixo (e a ressalva de que só `SD`/`HD`/`FHD`/`4K` foram
  confirmados contra o catálogo real,§5 do design), a máquina de estados da cascata, e
  qualquer achado real do passo de validação (T11). Atualizar a tabela de acompanhamento
  deste plano e a §6 do design.
- [ ] **Passo 6: Com o "sim" do dono** (ou a autorização permanente já concedida para tags de
  versão), publicar a tag `v0.2.0`.

### T11: Validação do dono no celular, contra o provedor real

- [ ] **Passo 1:** Atualizar o app por cima da versão anterior (upgrade já validado no Plano
  1 — não precisa reconfirmar o mecanismo, só rodar de novo).
- [ ] **Passo 2:** Abrir a TV ao vivo, entrar num canal (ex. o mesmo "Warner Channel" usado no
  brainstorming) e confirmar: abre na maior qualidade disponível; o seletor de qualidade só
  lista as variantes que esse canal realmente tem (**[A VERIFICAR]** contra o catálogo real —
  ver design §5, algoritmo de sufixo pode ter variações não vistas ainda, tipo `UHD`).
- [ ] **Passo 3:** Provocar uma queda de qualidade de verdade (ex. desligar o Wi-Fi um
  instante com o canal tocando) e confirmar o selo de "Mudou para X" e que o vídeo continua.
- [ ] **Passo 4:** Trocar a qualidade manualmente e confirmar que, se essa também falhar, a
  cascata automática continua dali pra baixo (não trava na escolha manual).
- [ ] **Passo 5:** Testar um canal com só 1 variante de qualidade (se existir no catálogo real)
  e confirmar que uma falha vai direto pro erro terminal — tela de erro com botão "Tentar de
  novo", sem tentar "descer" pra lugar nenhum e sem ficar girando sozinho.
- [ ] **Passo 6:** Testar um canal sem programação (se existir) e confirmar "Sem programação"
  em vez de tela vazia.
- [ ] **Passo 7:** Girar o celular com o canal tocando e confirmar que o vídeo não reinicia
  (tela cheia entra suave, sem re-bufferizar do zero).
- [ ] **Passo 8:** Favoritar um canal, filtrar por Favoritos na aba Canais, e confirmar que
  aparece lá.
- [ ] **Passo 9:** Abrir Configurações pelo OSD, mudar o tempo de buffer, **trocar de canal**
  (o valor novo só vale a partir do próximo canal aberto) e confirmar que uma queda de
  qualidade nesse canal novo respeita o valor novo (aproximadamente — não precisa cronometrar
  com precisão de segundo).
- [ ] **Passo 10:** Com um canal tocando, trocar de app (ou bloquear a tela) e confirmar que o
  vídeo/áudio para; voltar ao app e confirmar que o canal reabre (do zero, sem retomar
  posição — é transmissão ao vivo).
