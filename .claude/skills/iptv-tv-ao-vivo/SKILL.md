---
name: iptv-tv-ao-vivo
description: Agrupamento de canais por qualidade (SD/HD/FHD/4K), maquina de estados da cascata de troca automatica/manual de qualidade, OSD e layout condicional por orientacao da tela de TV ao vivo. Use antes de tocar em aovivo/CanalAgrupado.kt, aovivo/CascataQualidade.kt, aovivo/PlayerAoVivo.kt, ui/aovivo/*.kt ou ui/configuracoes/*.kt.
---

# TV ao vivo: agrupamento, cascata de qualidade, OSD e Configurações

## Quando usar esta skill

Antes de mexer em `aovivo/CanalAgrupado.kt` (agrupamento por qualidade), `aovivo/
CascataQualidade.kt` (máquina de estados), `aovivo/PlayerAoVivo.kt` (ponte com o ExoPlayer),
`ui/aovivo/AoVivoTela.kt`, `ui/aovivo/Osd.kt`, `ui/aovivo/CanaisAba.kt`, `ui/aovivo/
ProgramacaoAba.kt` ou `ui/configuracoes/*.kt` — ou antes de implementar qualquer feature nova
que dependa de canal agrupado, troca de qualidade ou tempo de buffer configurável.

## Agrupamento de canais por qualidade (T1, `CanalAgrupado.kt`)

- O provedor expõe cada qualidade de um canal como uma entrada separada em
  `get_live_streams`, com o nome terminando num sufixo de qualidade (ex. `WARNER CHANNEL
  FHD`, `WARNER CHANNEL HD`, `WARNER CHANNEL SD`). O SD é sempre explícito, nunca "sem
  sufixo" (confirmado contra o catálogo real do dono, design §"TV ao vivo" > "Qualidade do
  canal").
- `qualidadeDoNome`/`nomeBase` usam a mesma regex ```(?i)\s+(4K|FHD|HD|SD)$``` (fim da
  string, case-insensitive) pra extrair a qualidade e produzir o nome sem sufixo.
- **[A VERIFICAR] só `SD`/`HD`/`FHD`/`4K` foram confirmados contra o catálogo real** (design
  §5, "Deixado para quem for construir"): o dono confirmou esses 4 sufixos ao vivo durante o
  brainstorming, mas não o catálogo completo. Se aparecer um canal com variação não coberta
  (`UHD`, `FULL HD`, sufixo no meio do nome), `qualidadeDoNome` retorna `null` e
  `agruparPorQualidade` cai no fallback `Qualidade.SD` (ver código) — o canal não quebra, mas
  a variante pode ficar rotulada errado. Não presuma que a lista de sufixos está completa só
  porque os testes sintéticos passam.
- **Chave de agrupamento é `nomeBase + categoriaId`, não só o nome** — decisão deliberada do
  design, pra não juntar por engano dois canais diferentes com o mesmo nome-base em
  categorias distintas. Não "simplificar" para agrupar só por nome.
- `epg_channel_id` é o **mesmo** entre as variantes de qualidade do mesmo canal lógico
  (confirmado com dados reais, skill `iptv-xtream-carga` item 4) — a aba Programação e
  `AoVivoViewModel.programaAtual`/`programacaoDoDia` usam o `epgId` de qualquer variante do
  canal agrupado (hoje, a do primeiro item do grupo), são todos iguais.

## Máquina de estados da cascata de qualidade (T5, `CascataQualidade.kt`)

- `canal.variantes` já vem ordenada da maior pra menor qualidade (contrato de T1) — descer
  qualidade é sempre avançar `indiceAtual` em 1; a pior é o último índice.
- Eventos emitidos (`EventoCascata`, sem replay no `SharedFlow`): `Tocando(variante)` ao
  iniciar/reiniciar, `Trocou(de, para)` a cada degradação ou troca manual, `FalhaTerminal`
  quando a pior qualidade também falha.
- `aoTravar()` (re)inicia o timer de `tempoBufferMs` — chamar de novo reinicia do zero
  (**não** é um orçamento único pra cascata inteira: no pior caso, 4 variantes podem levar
  `4 × tempoBufferMs` até estabilizar no SD). `aoEstabilizar()` cancela o timer pendente sem
  descer. `aoErroDefinitivo()` desce na hora, sem esperar o timer — HTTP 404/stream não
  encontrado/codec não suportado não merecem esperar o buffer.
- Falha na pior qualidade (`FalhaTerminal`): a cascata **para** e só volta a tentar com
  `tentarNovamente()` (chamado pelo botão manual na tela) — nunca retry automático sem
  limite. *Por quê:* a conta do provedor permite só 2 conexões simultâneas (skill
  `iptv-xtream-carga`), um retry automático sem fim arriscaria esgotar esse limite.
- **Achado real confirmado na T9 (não é suposição, é o comportamento observado do
  `PlayerAoVivo`)**: `escolherManualmente(qualidade)` precisa **emitir `EventoCascata.Trocou`
  na hora**, não só trocar `indiceAtual` internamente. O player (T7/`PlayerAoVivo`) só troca
  o `MediaItem` de fato reagindo a `eventos`; se `escolherManualmente` só atualizasse o
  índice sem emitir, o seletor manual do OSD ficaria mudo (o usuário clica, nada toca). Ver
  o comentário em `CascataQualidade.escolherManualmente` e o selo "Mudou para X" do OSD, que
  depende do mesmo evento.
- Troca manual **não desliga** a automática: só muda o ponto de partida (`indiceAtual`) e
  zera `falhouDeFormaTerminal` — se a qualidade escolhida manualmente também falhar, a
  cascata automática continua degradando dali pra baixo normalmente.

## OSD e layout condicional por orientação (T9/T10, `Osd.kt`/`AoVivoTela.kt`)

- O `Osd` é um Composable único, reusado em pé e deitado — ele já traz seu próprio "Voltar",
  seletor de qualidade, canal anterior/próximo, atalho pra grade de programação (overlay) e
  atalhos pra "Gerenciar contas"/"Configurações". `AoVivoTela` nunca duplica essa lógica: só
  decide o que fica visível **em volta** dele.
- **Orientação (Passo 1, T10)**: `LocalConfiguration.current.orientation ==
  Configuration.ORIENTATION_LANDSCAPE` decide o layout. Em paisagem, o `Box` do vídeo vira
  `Modifier.fillMaxSize()` (em vez de `.height(220.dp)`) e o `TextButton("Voltar")` externo +
  `TabRow` + as abas Canais/Programação somem — só vídeo + `Osd` por cima. Em retrato, nada
  muda (tela dividida de sempre).
- **Acerto que evitou um `DisposableEffect`/listener próprio**: `LocalConfiguration.current`
  já recompõe a árvore sozinho quando a orientação muda, sem precisar observar nada
  manualmente — é só ler o valor dentro do `@Composable`.
- **Acerto que depende do T6**: o manifest já declara
  `android:configChanges="orientation|screenSize|screenLayout"` (T6) — a `Activity` não é
  recriada ao girar o celular, então o `PlayerView`/`ExoPlayer` (que vivem em `remember`
  dentro do `AndroidView`/`AoVivoViewModel`) não são recriados por causa da rotação. Girar o
  celular não derruba o vídeo em andamento. **Não remover esse `configChanges`** sem saber
  que vai reintroduzir esse problema.
- O overlay de grade de programação (`if (grade) { ... }`, atalho do OSD) é independente de
  orientação: ocupa a tela inteira nos dois casos, não precisa de ramificação própria.

## Configurações (T4/T10)

- `ConfiguracoesRepositorio` (T4) guarda o tempo de buffer em **milissegundos**
  (`tempoBufferMs()`/`definirTempoBufferSegundos(Int)`, padrão 8000ms = 8s). A UI
  (`ConfiguracoesTela`/`ConfiguracoesViewModel`, T10) trabalha em **segundos** — a conversão
  ms↔segundos fica isolada em `ConfiguracoesViewModel.tempoBufferSegundos()`
  (`ms / 1000`), não espalhada pela tela.
- `AoVivoViewModel.selecionarCanal` lê `app.configuracoes.tempoBufferMs()` a cada chamada
  (não guarda em cache) — uma troca feita em Configurações só vale a partir do **próximo**
  canal aberto, nunca retroage no canal já tocando.
- `ConfiguracoesTela` é acessível de dois lugares: o atalho "Configurações" do `Osd` (T9) e
  um botão "Configurações" na tela de Resumo, ao lado de "Gerenciar contas" (T10, decisão do
  design §"Configurações": "também acessível de onde 'Gerenciar contas' já é hoje"). A
  navegação (`ui/Navegacao.kt`) guarda qual tela abriu Configurações (`Resumo` ou `Ao vivo`)
  num `telaAntesDeConfiguracoes`, pra "Voltar" retornar pro lugar certo em vez de sempre pro
  Resumo.

## Pegadinhas cross-cutting achadas só na revisão final do Plano 2 (não visíveis tarefa a tarefa)

Três comportamentos do Media3/ExoPlayer que nenhuma revisão isolada de T5-T9 pegou, porque
cada tarefa sozinha parecia correta — só apareceram revisando a integração completa (T1-T10
juntas), confirmados lendo o bytecode real do Media3 1.11.1, não suposição:

- **`prepare()` chamado enquanto o player já está em `STATE_BUFFERING` não dispara
  `onPlaybackStateChanged` de novo** (o Media3 só avisa quando o estado muda, e
  `setMediaSourcesInternal` já mascara pra `BUFFERING` antes do `prepare()` rodar). Isso
  quebrava o timer da cascata: ao vencer, `PlayerAoVivo` trocava de `MediaItem` mas a
  cascata nunca sabia que a variante nova também estava "carregando", e ficava sem timer
  pra próxima tentativa. **Correção**: `PlayerAoVivo.abrir()` chama `cascata.aoTravar()`
  explicitamente depois de `setMediaItem`/`prepare`/`play` — não confie só nos callbacks do
  `Player.Listener` pra saber que uma troca de variante começou.
- **`exoPlayer.pause()` sozinho não é "parar de verdade" em `ON_STOP`.** Pausar não cancela
  o listener, a coleta de `cascata.eventos` nem o timer da cascata — se o timer vencer com o
  app em segundo plano, o coletor ainda reage a `Trocou` chamando `play()` de novo (áudio
  tocando minimizado + conexão aberta, viola o limite de 2 conexões do provedor).
  **Correção**: `PlayerAoVivo.parar()` (cancela `jobColeta`, chama `aoEstabilizar()` na
  cascata atual, só então `exoPlayer.stop()`) é o que `pausar()` deve chamar, nunca
  `exoPlayer.pause()` isolado.
- **`EventoCascata.Trocou` carrega `VarianteQualidade`, não só `Qualidade`** (mudou na
  revisão final; T5 original carregava só a qualidade). Motivo: se um `CanalAgrupado` tiver
  duas variantes da MESMA qualidade (provedor duplicando o canal, ou um sem sufixo caindo no
  fallback `Qualidade.SD` ao lado de um "X SD" explícito no mesmo grupo), resolver a variante
  de novo por `first { it.qualidade == evento.para }` sempre pega a primeira — se essa
  travou, a cascata "desce" pra ela de novo, repetindo o mesmo stream que já falhou.
  `CascataQualidade` agora emite a `VarianteQualidade` exata (`varianteAtual()`, pelo índice
  real), e `PlayerAoVivo`/`Osd` usam `evento.para` direto, sem resolver de novo.
- **[A VERIFICAR] mesma família de bug ainda existe no caminho MANUAL**:
  `escolherManualmente(qualidade: Qualidade)` continua resolvendo por
  `indexOfFirst { it.qualidade == qualidade }` — num grupo com duas variantes da mesma
  qualidade, escolher manualmente pode reselecionar a que já falhou, e o OSD mostra os dois
  botões daquela qualidade destacados juntos. Não corrigido ainda (achado da re-revisão do
  fix wave, fora do escopo daquela rodada) — correção análoga à do `Trocou` seria
  `escolherManualmente` passar a receber `VarianteQualidade`/índice em vez de `Qualidade`.
- **`keepScreenOn = true` no `PlayerView`** (`AoVivoTela.kt`) é obrigatório — sem ele a tela
  apaga durante a reprodução (ninguém toca a tela só assistindo), dispara `ON_STOP`, e o
  vídeo para como se o usuário tivesse saído do app.

## Como validar

- Automatizado (dados sintéticos): `./scripts/gradle.ps1 testDebugUnitTest`. Cobre o
  agrupamento por sufixo, a máquina de estados da cascata (timers, erro definitivo, troca
  manual, falha terminal) e a conversão ms/segundos de Configurações.
- Manual, no celular do dono (sem emulador nesta máquina — ver CLAUDE.md do projeto): abrir
  um canal com várias qualidades, deixar travar de propósito (ex. Wi-Fi ruim) e confirmar que
  desce de qualidade sozinho mostrando o selo; girar o celular e confirmar tela cheia sem
  cortar o vídeo; abrir Configurações pelos dois caminhos (OSD e Resumo) e confirmar que o
  valor salvo é o mesmo nos dois.
