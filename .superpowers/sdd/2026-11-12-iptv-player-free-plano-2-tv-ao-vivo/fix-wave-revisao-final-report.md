# Fix wave única — revisão final do Plano 2 (TV ao vivo)

Commit: `7248a12` — `fix: 3 criticos + 6 importantes da revisao final do Plano 2 (TV ao vivo)`
(autor/committer: Marcos Ronaldo <marcos.ronaldo@gmail.com>, sem linha de co-autoria, conforme
regra do projeto).

Skill lida antes de tocar em qualquer arquivo: `.claude/skills/iptv-tv-ao-vivo/SKILL.md`.
Global Constraints lidas em `docs/superpowers/plans/2026-11-12-iptv-player-free-plano-2-tv-ao-vivo.md`.

## Os 9 achados

### Crítico 1 — Tela apaga durante a reprodução
**Arquivo:** `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoTela.kt:124-133`
**Mudança:** dentro do `factory` do `AndroidView`, adicionado `keepScreenOn = true` no mesmo
`PlayerView(context).apply { ... }` que já seta `player = viewModel.exoPlayer`.
**Confirmação:** leitura do diff — a propriedade está dentro do `.apply` certo, não um bloco
separado. Não há teste automatizado de UI para isso (é comportamento de Android real,
`keepScreenOn` não tem efeito observável em `testDebugUnitTest`/Robolectric sem instrumentação) —
fica para validação manual no celular (T11, já prevista no plano).

### Crítico 2 — Timer de buffer não reinicia na qualidade nova
**Arquivo:** `app/src/main/java/io/github/pagmovel/iptvplayer/aovivo/PlayerAoVivo.kt:72-98`
**Mudança:** no coletor de `cascata.eventos` dentro de `abrir()`, após
`exoPlayer.setMediaItem(...)` + `prepare()` + `play()`, chamada explícita a
`cascata.aoTravar()`. Mantido o `when` resolvendo `variante` a partir do evento (agora também
simplificado pelo fix 9).
**Confirmação:** não há teste unitário de `PlayerAoVivo` no projeto (não existe
`PlayerAoVivoTest.kt` — confirmado com `find` antes de editar), porque a classe depende de
`ExoPlayer` real (precisa de Android/Robolectric com player de verdade, fora do escopo dos
testes JVM puros do projeto). Confirmação é por leitura do fluxo: `aoTravar()` (re)inicia o
timer; o `STATE_READY` seguinte do ExoPlayer cancela via `aoEstabilizar()` normalmente (já
testado em `CascataQualidadeTest`). Resta validação manual (T11).

### Crítico 3 — Vídeo pode voltar a tocar sozinho em segundo plano
**Arquivos:**
- `app/src/main/java/io/github/pagmovel/iptvplayer/aovivo/PlayerAoVivo.kt:105-115` — novo
  `fun parar()`: cancela `jobColeta`, chama `cascataAtual?.aoEstabilizar()`, chama
  `exoPlayer.stop()`.
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoViewModel.kt:237-242` —
  `pausar()` agora chama `playerAoVivo.parar()` em vez de `playerAoVivo.exoPlayer.pause()`
  direto.
**Confirmação:** leitura do diff — `parar()` tem as 3 ações pedidas, na ordem que evita a
corrida (cancela a coleta antes de tudo, então nenhum evento pendente pode mais chamar
`play()`). `AoVivoViewModelTest.kt` não testa `pausar()`/`retomar()` (só a lógica pura de
filtro/janela — confirmado por grep antes de editar), então a suíte não cobre isso
diretamente; comportamento fica para validação manual (minimizar o app com a cascata
travando).

### Importante 4 — `FalhaTerminal` não para o player
**Arquivo:** `PlayerAoVivo.kt:93-97` (branch `else` do `if (variante != null)`)
**Mudança:** adicionado `exoPlayer.stop()` no branch que antes não fazia nada quando
`variante == null` (ou seja, `FalhaTerminal`).
**Confirmação:** leitura do diff — branch `else` cobre exatamente `EventoCascata.FalhaTerminal`
(o único caso que produz `variante == null` no `when`).

### Importante 5 — Botão "Tentar de novo" coberto pelo OSD
**Arquivo:** `AoVivoTela.kt:143-171`
**Mudança:** o bloco do aviso de `FalhaTerminal` foi movido para **depois** da chamada a
`Osd(...)`, dentro do mesmo `Box`. Compose desenha em ordem e o último filho recebe o toque
primeiro quando há sobreposição — então o aviso (com o botão) agora fica por cima do OSD.
**Confirmação:** leitura do diff — ordem trocada, nenhuma outra lógica alterada.

### Importante 6 — `ProgramacaoAba` mostra grade do canal anterior
**Arquivo:** `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/ProgramacaoAba.kt:35`
**Mudança:** `produceState(initialValue = emptyList<Programa>(), dia)` →
`produceState(initialValue = emptyList<Programa>(), dia, viewModel.canalAtual)`.
**Confirmação:** leitura do diff. Não há teste de Compose para recomposição de `produceState`
no projeto; a correção é a chave de recomposição documentada no próprio Compose (múltiplas
chaves reiniciam o efeito quando qualquer uma muda).

### Importante 7 — Paisagem sem canal selecionado sem saída
**Arquivo:** `AoVivoTela.kt:112-161`
**Mudança:** `val telaCheia = emPaisagem && canalAtual != null`, substituindo `emPaisagem` nas
3 condições que controlavam visibilidade (botão "Voltar", modificador de tamanho do vídeo,
bloco `TabRow` + abas).
**Confirmação:** leitura do diff — as 3 ocorrências de `emPaisagem` controlando o que é
mostrado foram trocadas por `telaCheia`; `emPaisagem` continua existindo só para derivar
`telaCheia`.

### Importante 8 — Filtro duplicado entre UI e ViewModel
**Arquivos:**
- `AoVivoViewModel.kt:107-113` — `filtroCategoriaAtual`/`filtroSomenteFavoritosAtual`
  expostos como `StateFlow` público via `.asStateFlow()` sobre os `MutableStateFlow` privados
  já existentes.
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/CanaisAba.kt:44-50` — os
  `remember { mutableStateOf(...) }` locais foram substituídos por
  `collectAsState()` sobre os novos `StateFlow` do ViewModel; os 3 `onClick` dos chips não
  reatribuem mais `categoriaSelecionada`/`somenteFavoritos` localmente (eram `val` agora,
  não podem — o compilador pegou isso na primeira tentativa de build, ver abaixo), só chamam
  `viewModel.filtrarPorCategoria(...)`/`filtrarFavoritos(...)`.
- Imports `mutableStateOf`/`setValue` removidos de `CanaisAba.kt` (ficaram sem uso).
**Confirmação:** leitura do diff + build. A primeira rodada de `assembleDebug` falhou com
`'val' cannot be reassigned` nas linhas que ainda atribuíam `categoriaSelecionada = ...`
dentro dos `onClick` — exatamente o sinal de que o ViewModel passou a ser a única fonte de
verdade (um `var` local não existe mais). Corrigido removendo as atribuições redundantes.
Build e suíte completa voltaram a passar depois.

### Importante 9 — Qualidade repetida resolve a variante errada
**Arquivos:**
- `app/src/main/java/io/github/pagmovel/iptvplayer/aovivo/CascataQualidade.kt` — `EventoCascata.Trocou`
  passa a carregar `VarianteQualidade` (`de`/`para`) em vez de `Qualidade`; `escolherManualmente`
  e `descerQualidade` emitem a variante inteira.
- `PlayerAoVivo.kt:74-82` — `is EventoCascata.Trocou -> evento.para` substitui
  `canal.variantes.first { it.qualidade == evento.para }`.
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/Osd.kt:82,131` — `eventoAtual.para`
  → `eventoAtual.para.qualidade` (destaque do seletor) e `evento.para.rotulo` →
  `evento.para.qualidade.rotulo` (selo "Mudou para X").
- `app/src/test/java/io/github/pagmovel/iptvplayer/aovivo/CascataQualidadeTest.kt` — as 4
  asserções `EventoCascata.Trocou(Qualidade.X, Qualidade.Y)` passaram a usar
  `EventoCascata.Trocou(canal.variantes[i], canal.variantes[j])`, com os índices batendo com
  a ordem de `canalCom(...)` de cada teste (ex. `canalCom(FHD, HD, SD)` → `variantes[0]`=FHD,
  `[1]`=HD, `[2]`=SD).
**Confirmação:** suíte completa rodada depois do ajuste — `CascataQualidadeTest` com 11/11
testes passando (contagem real abaixo), continuam provando o comportamento real da máquina de
estados (timer, erro definitivo, escolha manual, falha terminal), só a forma do evento mudou
de `Qualidade` solta para a `VarianteQualidade` que o player de fato usa — o que é estritamente
mais forte, não "só compila": antes um teste que comparasse duas variantes de mesma qualidade
não distinguiria a variante errada, agora a asserção é sobre o objeto exato.

## Evidência de build/testes (contagem real, sem filtro)

Comando: `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`

Primeira rodada: `BUILD FAILED` — 6 erros de compilação em `CanaisAba.kt` (`'val' cannot be
reassigned`, linhas 63/64/75/76/87/88), causados pelos `onClick` dos chips que eu tinha
deixado reatribuindo localmente `categoriaSelecionada`/`somenteFavoritos` (agora `val`
delegados do ViewModel). Corrigido removendo as 3 reatribuições redundantes (fix 8 já cobria
isso pelo ViewModel).

Segunda rodada: `BUILD SUCCESSFUL in 3m 38s`, 47 tasks (11 executadas, 36 up-to-date).

Contagem real dos relatórios JUnit XML (`app/build/test-results/testDebugUnitTest/*.xml`):

| Suíte | Testes |
|---|---|
| EsqueletoTest | 1 |
| CanalAgrupadoTest | 5 |
| **CascataQualidadeTest** | **11** |
| CargaCatalogoTest | 4 |
| CargaWorkerTest | 2 |
| ConfiguracoesRepositorioTest | 2 |
| ContasRepositorioTest | 5 |
| LinkXtreamTest | 4 |
| CatalogoDaoTest | 10 |
| MigracaoBancoTest | 1 |
| LeitorXmltvTest | 7 |
| AoVivoViewModelTest | 16 |
| ConfiguracoesViewModelTest | 2 |
| MapeamentoTest | 7 |
| XtreamApiTest | 10 |
| **Total** | **87** |

**87 testes, 0 falhas, 0 erros, 0 pulados.** `assembleDebug` gerou
`app/build/outputs/apk/debug/app-debug.apk` (16.689.315 bytes, timestamp confere com o run).

## Arquivos alterados (commit `7248a12`)

- `app/src/main/java/io/github/pagmovel/iptvplayer/aovivo/CascataQualidade.kt`
- `app/src/main/java/io/github/pagmovel/iptvplayer/aovivo/PlayerAoVivo.kt`
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoTela.kt`
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoViewModel.kt`
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/CanaisAba.kt`
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/Osd.kt`
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/ProgramacaoAba.kt`
- `app/src/test/java/io/github/pagmovel/iptvplayer/aovivo/CascataQualidadeTest.kt`

Nenhum outro arquivo do repositório foi tocado. As alterações não commitadas que já existiam
no repositório antes desta tarefa (plano `.md` e os arquivos de `.superpowers/sdd/.../` do
SDD das tarefas T1–T10) não são desta fix wave e foram deixadas como estavam.

## Autorrevisão

- Os 9 achados têm mudança de código correspondente, identificável no diff: sim, um a um,
  listados acima com arquivo:linha.
- Suíte completa passa (contagem real, sem filtro): sim, 87/87, 0 falhas/erros.
- `assembleDebug` passa: sim, APK gerado.
- Testes de T5 ajustados pro fix 9 ainda provam o comportamento real da cascata: sim — a
  máquina de estados continua sendo exercitada da mesma forma (timers, erro definitivo,
  escolha manual, falha terminal, retomada); só o tipo do payload do evento mudou, e ficou
  mais preciso (compara a variante exata, não só o rótulo de qualidade).
- `PlayerAoVivo.abrir(canal: CanalAgrupado, ...)`: o parâmetro `canal` ficou sem uso dentro do
  corpo da função depois do fix 9 (antes era usado para `canal.variantes.first { ... }`).
  Deixado como está — a assinatura é chamada por `AoVivoViewModel.selecionarCanal` passando
  `canal`, e um parâmetro de função não usado não é erro nem warning padrão do Kotlin/ktlint
  deste projeto (build limpo confirma). Não vale a pena quebrar a assinatura pública por isso
  numa fix wave já extensa.

## Dúvidas / preocupações

- Críticos 1 e 3 (keepScreenOn, segundo plano) e Crítico 2 (timer na troca de variante) não
  têm teste automatizado que os exercite de ponta a ponta — dependem de `ExoPlayer` real ou
  de comportamento do Android (apagar tela, ciclo de vida em background), fora do alcance dos
  testes JVM puros do projeto (confirmei que não existe `PlayerAoVivoTest.kt`, e
  `AoVivoViewModelTest.kt` não cobre `pausar()`/`retomar()`). A lógica foi revisada por leitura
  cuidadosa do fluxo e confirmada contra o comportamento documentado do Media3 citado no
  achado, mas a validação de fato é manual, no celular do dono (T11, já prevista no plano).
  Sinalizando para a re-revisão não assumir "testado" nesses 3 pontos além do que a leitura de
  código permite.
- Nenhum dos 9 achados ficou parcial ou sem solução; não houve necessidade de escalar.
