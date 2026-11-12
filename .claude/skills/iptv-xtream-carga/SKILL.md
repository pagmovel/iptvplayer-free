---
name: iptv-xtream-carga
description: Formato real da API Xtream Codes do provedor usado neste projeto, e como o app carrega catálogo/programação em segundo plano sem travar e sem perder dados numa falha parcial. Use antes de tocar em qualquer coisa sob xtream/, carga/ ou programacao/.
---

# API Xtream e carga do catálogo

## Quando usar esta skill

Antes de mexer em `xtream/XtreamApi.kt`, `xtream/Mapeamento.kt`, `xtream/JsonCampos.kt`,
`carga/CargaCatalogo.kt`, `carga/CargaWorker.kt` ou `programacao/LeitorXmltv.kt` — ou antes de
implementar qualquer tela/feature nova que fale com o provedor (detalhe de série, EPG curto
por canal, catch-up). Lê rápido, mas lê antes: várias pegadinhas abaixo já custaram um ciclo
de "ver falhar" de verdade numa tarefa anterior.

## Formato real do provedor (confirmado em 2026-11-10, design §3.1)

Coletado só com leitura (`temp/coletar_amostras.py`, nunca versionado — ver CLAUDE.md do
projeto). Não é documentação genérica do Xtream Codes: são os valores **deste** provedor.

- Base: `player_api.php?username=...&password=...&action=...`. Sem `action`, autentica.
- Ações usadas pela carga (T9, `CargaCatalogo`): `get_live_categories`, `get_vod_categories`,
  `get_series_categories`, `get_live_streams`, `get_vod_streams`, `get_series`. Resposta: um
  array JSON (lido em streaming, ver "Acertos" abaixo).
- `xmltv.php` (sem `action`): XML da programação, **8,2 MB** no provedor real. Horários em
  UTC (`+0000`), com `start`/`stop` no formato `AAAAMMDDHHmmss +0000` e também os atributos
  `start_timestamp`/`stop_timestamp` (não usados ainda pelo parser).
- Volumes reais (mudam com o tempo, mas dão a ordem de grandeza pra validar uma carga):
  **39 categorias ao vivo / 946 canais**; **76 categorias de filmes / 16.122 filmes** (6,3 MB
  de JSON); **39 categorias de séries / 8.714 séries** (7,6 MB de JSON).
- Conta: `max_connections = 2`; formatos aceitos `m3u8`, `ts`, `rtmp`; servidor XUI.
- `auth`/`status` só aparecem dentro de `user_info` na resposta sem `action` (autenticação):
  `auth` **1** autenticou, **0** não (usuário/senha errados); `status` precisa ser
  `"Active"` — outros valores (`"Expired"`, `"Banned"`, `"Disabled"`) significam conta válida
  mas inativa.

## Pegadinhas confirmadas (não suponha o contrário sem reler o código)

1. **`get.php` (lista M3U) redireciona para `www.google.com`, não para o conteúdo.** Testado
   nos 4 servidores do provedor, nos formatos TS e HLS, com qualquer User-Agent. Por isso
   `XtreamApi` usa `followRedirects(false)` e qualquer `3xx` vira
   `FalhaXtream.RespostaInvalida` (nunca segue o redirect) — ver `XtreamApiTest`, teste
   "redirecionamento para outro host vira RespostaInvalida". É também por isso que o design
   descartou login por M3U como não-objetivo: o provedor parece bloquear esse caminho.
   **[INCONCLUSIVO]**: só testado a partir da rede desta máquina, não do celular do dono.

2. **Tipos inconsistentes no JSON**: o mesmo campo vem às vezes como número, às vezes como
   string numérica (ex.: `"auth":1` vs `"auth":"1"`), e pode vir ausente, `null` ou string
   vazia. `JsonCampos.kt` centraliza a tolerância num único helper privado
   (`primitivoOuNull`), e os 4 extratores públicos (`texto`, `long`, `int`, `decimal`) leem
   `JsonPrimitive.content` sem checar `isString` antes de converter — não escreva um parser
   novo que confia no tipo declarado do JSON.

3. **`get_series_info` traz `seasons` vazio**, mesmo quando a série tem episódios de verdade
   — os episódios aparecem em `episodes["1"]`, `episodes["2"]`, etc. **Quem for implementar
   a tela de detalhe de série (fora do escopo desta tarefa) precisa montar a lista de
   temporadas a partir das chaves de `episodes`, nunca a partir de `seasons`.** Isso ainda
   não está implementado no código — é um achado de exploração (design §3.1), não um
   comportamento já coberto por teste.

4. **Ids de EPG repetidos no XMLTV**: o mesmo `epg_channel_id` aparece em mais de um `<channel>`
   do XMLTV (ex.: a versão FHD e a versão HD do mesmo canal usam o mesmo id). Hoje isso não
   quebra nada porque `Programa` grava por `(contaId, epgId, inicio)` e aceita duplicata entre
   canais diferentes com o mesmo `epgId` — mas **quem for casar canal × programação (tela de
   TV ao vivo, Fase 2) vai achar mais de um canal puxando a mesma entrada de EPG**, e isso é
   esperado, não bug.

5. **`get_short_epg` voltou vazio no primeiro canal testado** — nem todo canal tem
   programação. O design já prevê isso ("Canal sem programação: o app mostra 'Sem
   programação' em vez de ficar vazio ou travar"); ainda não implementado.

6. Servidor não tem TLS (`http://`) — por isso o manifest tem
   `android:usesCleartextTraffic="true"`; isso não é uma pegadinha a corrigir, é intencional.

7. **`abrirXmltv` não convertia `IOException`/`XmlPullParserException` da leitura do corpo em
   `FalhaXtream`** (2026-11-12, achado real em uso de verdade pelo dono: sincronizar deu
   "Falha ao atualizar." genérico). `lerLista` já envolvia a leitura em try/catch;
   `abrirXmltv` não. Corrigido (commit `c62d580`) envolvendo
   `corpo.byteStream().use { ler(it) }` do mesmo jeito. **Testei contra o XMLTV real de uma
   conta de verdade** (7,2 MB, baixado via `.env` de teste do dono) rodando `lerXmltv` direto
   nele: 19.060 programas aceitos, 0 exceção — então XML malformado **não** era a causa nesta
   conta especificamente; a correção cobre o outro cenário real (conexão cair no meio do
   download de um arquivo desse tamanho em rede móvel), e qualquer causa futura agora aparece
   como mensagem específica em vez do fallback genérico, em vez de ficar sem pista nenhuma.

8. **Script PowerShell lendo chave acentuada ("usuário") de um `.env` por regex deu `404`
   falso** — o `player_api.php` funcionava perfeitamente, mas um regex `^usuário=` no código
   do script (não no arquivo) não bateu de forma confiável por causa de como o PowerShell 5.1
   grava o `.ps1` temporário vs. o encoding real do `.env`, deixando `$usuario` vazio só
   nessa chamada. **Lição: ao ler um `.env` com chave acentuada num script desses, leia por
   posição da linha** (`Get-Content | Where-Object { não é comentário/vazio }`, pegar
   `[0]`/`[1]`/`[2]`), nunca por regex que depende do caractere acentuado bater.

## Acertos (não desfazer sem motivo)

- **Streaming em lotes, nunca a lista inteira na memória**: `XtreamApi.lerLista(acao,
  tamanhoLote=500, aoLote)` decodifica o JSON como sequência (`Json.decodeToSequence`,
  `DecodeSequenceMode.ARRAY_WRAPPED`) e entrega lotes; `LeitorXmltv.lerXmltv` usa
  `XmlPullParser` evento a evento, também em lotes. Dado os volumes reais (16 mil filmes, 8,2
  MB de XMLTV), montar a lista inteira antes de gravar era exatamente o anti-objetivo que
  motivou o projeto (design, §2).
- **Duas transações separadas em `CargaCatalogo.executar`**: uma para
  categorias+canais+filmes+séries, outra para a programação. Falha na programação não desfaz
  o catálogo recém-carregado, e falha em qualquer etapa do catálogo desfaz o catálogo inteiro
  (nunca "pela metade"). Ver os 4 cenários de `CargaCatalogoTest` para o motivo de cada
  transação ter esse limite exato.
- **`followRedirects(false)` no `OkHttpClient` de `XtreamApi`** — sem isso, o redirecionamento
  do `get.php` (pegadinha 1) seria seguido silenciosamente para `www.google.com` e o app
  tentaria interpretar HTML como JSON/XMLTV.
- **Mensagem de erro traduzida num único lugar**: `FalhaXtream.mensagemUsuario()` (em
  `xtream/FalhaXtream.kt`) é a única tradução pt-BR dos 4 casos de falha, usada tanto pelo
  `CargaWorker` quanto pela tela de contas — evita duas traduções divergindo com o tempo.
- **Robolectric + MockWebServer + Room em memória**, sem emulador nem ambiente local de build
  (a máquina do dono não tem SDK Android): todo teste de `xtream/`, `carga/` e `conta/` roda
  assim, com dados **sintéticos** (nunca os de `temp/amostras/`, que não podem ir pro
  repositório público).
- **Infra do Robolectric**: o artefato `org.robolectric:nativeruntime-dist-compat` trava com
  HTTP 429 no Maven Central; a correção real foram dois ajustes (não um só): `exclusiveContent`
  em `settings.gradle.kts` (força esse módulo a vir só do espelho do Google, porque declarar o
  espelho como repositório extra solto não bastou) **e** a system property
  `robolectric.dependency.repo.url` em `app/build.gradle.kts` (o fetcher interno de
  `android-all`/`android-all-instrumented` do Robolectric não passa pela resolução de
  dependências do Gradle, então o `exclusiveContent` não alcança ele).

## Como validar

- Automatizado (dados sintéticos, todo PR): `./scripts/gradle.ps1 testDebugUnitTest`. Cobre
  `XtreamApiTest` (autenticação, falhas, redirecionamento), `MapeamentoTest` (tolerância de
  tipos), `LeitorXmltvTest` (janela de programação) e `CargaCatalogoTest`/`CargaWorkerTest`
  (transações e tradução de erro).
- Manual, no celular do dono, com a conta real (único jeito de validar contra o provedor de
  verdade — ver CLAUDE.md, "Sem ambiente local de build/emulador"): depois de cadastrar a
  conta e esperar a carga, o resumo deve mostrar números **próximos** de 946 canais, 16.122
  filmes e 8.714 séries (o provedor muda isso com o tempo, não é um valor fixo a bater exato).
