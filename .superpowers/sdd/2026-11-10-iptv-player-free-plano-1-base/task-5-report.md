# Relatório T5 — Cliente da API Xtream

## Status: DONE

Código implementado, testado de fato (não presumido) e commitado. Igual à T6, o comando
oficial `./scripts/gradle.ps1 testDebugUnitTest --tests '*XtreamApiTest'` ainda tropeça no
bloqueio externo do Maven Central (HTTP 429 num artefato do Robolectric que entra na
resolução do classpath de teste do módulo inteiro, mesmo filtrando a classe). A verificação
real foi feita pelo caminho alternativo (compilar + rodar com `java -cp` direto), e por isso
não marco `DONE_WITH_CONCERNS`: a execução é real nos dois casos, só o canal muda — e isso já
estava avisado no meu brief como esperado, não é um achado novo.

## O que foi feito

Arquivos (exatamente os do brief, mais a dependência de build que faltava):

- `app/src/main/java/io/github/pagmovel/iptvplayer/xtream/XtreamApi.kt` — `class XtreamApi`
  com `autenticar()`, `lerLista()`, `abrirXmltv()`, `urlAoVivo()`, `urlFilme()`,
  `urlEpisodio()`, conforme assinaturas exatas do brief. Usa os auxiliares já existentes de
  `JsonCampos.kt` (T6) — não criei nem movi nenhum auxiliar novo.
- `app/src/main/java/io/github/pagmovel/iptvplayer/xtream/FalhaXtream.kt` — `sealed class
  FalhaXtream : Exception()` com os 4 casos pedidos.
- `app/src/test/java/io/github/pagmovel/iptvplayer/xtream/XtreamApiTest.kt` — 8 testes com
  MockWebServer (os 6 do brief + 2 que acrescentei, ver "Autorrevisão").
- `app/build.gradle.kts` — adicionei `implementation(libs.okhttp)` e
  `testImplementation(libs.okhttp.mockwebserver)`. As entradas já existiam no catálogo
  (`gradle/libs.versions.toml`, feito na T1) mas não estavam nas `dependencies {}` do módulo;
  sem elas nada de OkHttp/MockWebServer compila. Não toquei no catálogo de versões.

## Passo a passo seguido

1. **Testes que falham**: escrevi `XtreamApiTest.kt` com as assinaturas e os JSONs exatos do
   brief antes de criar `XtreamApi.kt`/`FalhaXtream.kt`.
2. **Ver falhar**: nesse ponto o projeto não compilava (classes inexistentes) — falha real,
   não presumida.
3. **Implementar**: `FalhaXtream.kt` e `XtreamApi.kt`, conforme detalhado acima.
4. **Ver passar**: rodei de fato (duas vezes, antes e depois de uma limpeza de warning — ver
   "Verificação"). 8/8 testes passaram nas duas vezes.
5. **Commit**: `ccf65d2` — "feat: cliente da API Xtream com leitura em fluxo".

## Decisões de implementação (onde o brief deixava espaço)

- **`followRedirects(false)` dentro do próprio `XtreamApi`**, não delegado a quem monta o
  `OkHttpClient` de fora: a cada chamada, `executar()` faz
  `cliente.newBuilder().followRedirects(false).callTimeout(...).build()`. Assim o
  comportamento "não seguir redirect para outro host" vale mesmo que o `OkHttpClient`
  injetado não tenha sido configurado para isso — e dá para testar isso direto com
  `OkHttpClient()` puro no teste, sem precisar de um `OkHttpClient` especial só para esse
  caso.
- **Classificação de HTTP não-2xx/não-3xx como `ServidorIndisponivel`**: o brief só cita
  "HTTP 503" como exemplo; generalizei para qualquer resposta `!isSuccessful` que não seja
  redirecionamento (nenhum outro código aparece nos testes ou no design).
- **`urlFilme`/`urlEpisodio`**: o brief abrevia com "`/movie/...`"/"`/series/...`". Segui o
  padrão documentado da API Xtream Codes, no mesmo formato de `urlAoVivo`:
  `$servidor/movie/$usuario/$senha/$streamId.$extensao` e
  `$servidor/series/$usuario/$senha/$id.$extensao`. Não há teste dedicado a elas (nem no
  brief) — são interpolação de string de uma linha, sem branch; ponytail: trivial, sem teste.
- **Um `withContext(Dispatchers.IO)` por método público** (`autenticar`, `lerLista`,
  `abrirXmltv`), em vez de um por chamada de rede: cobre a chamada de rede inteira, a leitura
  do corpo e (em `lerLista`) a chamada ao callback `aoLote`, tudo na mesma dispatch — não
  precisa de outro `withContext` aninhado.
- **`Response.body` não é mais nulo a partir do OkHttp 5** (era `ResponseBody?` no OkHttp 4).
  O compilador avisou isso (`Elvis operator (?:) always returns the left operand`) na
  primeira rodada — removi o branch morto de "resposta sem corpo" em vez de ignorar o aviso.

## Verificação

**Rodei de fato, nas duas pontas:**

1. `./scripts/gradle.ps1 testDebugUnitTest --tests '*XtreamApiTest'` — `compileDebugKotlin` e
   `compileDebugUnitTestKotlin` **compilaram com sucesso** (confirma que o código e o teste
   batem com o que o Kotlin aceita). `testDebugUnitTest` falhou na resolução do classpath,
   com a mensagem exata: `Could not download nativeruntime-dist-compat-1.0.19.jar
   (org.robolectric:nativeruntime-dist-compat:1.0.19)` — o mesmo bloqueio que a T6 já tinha
   achado e que o meu brief avisava que podia continuar ativo. Confirmei que é esse motivo
   específico, não um bug meu: o log de erro cita literalmente esse artefato.
2. **Caminho alternativo** (mesmo da T6): classes já compiladas pelo Gradle
   (`app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes` e
   `.../debugUnitTest/compileDebugUnitTestKotlin/classes`, montadas como bind mount, reais no
   disco) + jars de produção baixados direto do Maven Central (não bloqueado para esses
   artefatos específicos — só o do Robolectric está): `okhttp-jvm-5.5.1`, `mockwebserver3-
   5.5.1`, `mockwebserver-5.5.1` (o shim legado com `okhttp3.mockwebserver.*`), `okio-jvm-
   3.18.1`, `kotlinx-coroutines-core-jvm-1.11.0`, `kotlinx-coroutines-test-jvm-1.11.0`,
   `annotations-23.0.0`, mais os jars já baixados pela T6 (`kotlin-stdlib`, `kotlinx-
   serialization-core-jvm`, `kotlinx-serialization-json-jvm`, `junit`, `hamcrest-core`) —
   versões exatas conferidas nos `.pom` reais de cada artefato no Maven Central (não supus
   nenhuma transitiva). Jars em `temp/verificacao-t5/libs/` (fora do git, `.gitignore` já
   cobre `temp/`, confirmado com `git check-ignore -v temp`).

   Rodei `java -cp "<classes principais>:<classes de teste>:<jars>/*"
   org.junit.runner.JUnitCore io.github.pagmovel.iptvplayer.xtream.XtreamApiTest` dentro do
   mesmo container `cimg/android:2026.10`. Resultado real, copiado do terminal:

   ```
   JUnit version 4.13.2
   ........
   Time: 13.488

   OK (8 tests)
   ```

   Depois de remover o branch morto do `Response.body` (ver acima), recompilei
   (`compileDebugUnitTestKotlin`, sem warnings) e rodei de novo pelo mesmo caminho: **8/8
   de novo**. Também rodei `MapeamentoTest` (T6) pelo mesmo classpath para garantir que não
   quebrei nada no pacote `xtream`: **7/7**, sem regressão.

## Autorrevisão

- **Completude**: `XtreamApi` e `FalhaXtream` têm exatamente os membros pedidos no brief, com
  as assinaturas literais (`autenticar(): InfoConta`, `lerLista(acao, tamanhoLote = 500,
  aoLote): Int`, `abrirXmltv<T>(ler): T`, as 3 funções de url, os 4 casos de `FalhaXtream`).
- **Os 5 casos de falha pedidos para revisão** — todos cobertos por teste e verificados
  passando:
  - `auth = 0` → `CredenciaisInvalidas` (teste `auth 0 vira CredenciaisInvalidas`).
  - status inativo (`"Expired"`) → `ContaInativa` com `.status` certo (teste `status Expired
    vira ContaInativa`).
  - HTTP 503 → `ServidorIndisponivel` (primeira metade de `HTTP 503 e timeout viram
    ServidorIndisponivel`).
  - timeout → `ServidorIndisponivel` (segunda metade do mesmo teste; usei `readTimeout` de
    200ms num `OkHttpClient` só desse caso, com o servidor atrasando os headers em 2s — testa
    o comportamento real sem o teste precisar esperar os 20s do `callTimeout` de
    `autenticar()`).
  - resposta HTML → `RespostaInvalida` (teste `resposta HTML vira RespostaInvalida`).
- **2 testes que acrescentei, fora da lista literal do brief**, por cobrirem comportamento
  exigido no Passo 3 da implementação que de outro modo ficaria sem nenhuma verificação:
  - `redirecionamento para outro host vira RespostaInvalida`: testa o `followRedirects(false)`
    em si (3xx real do MockWebServer) — é o requisito mais citado no brief/contexto ("não
    seguir redirect para `www.google.com`") e nenhum dos 6 testes literais gera uma resposta
    3xx de verdade (a de "resposta HTML" é um 200 com corpo errado, cenário diferente).
  - `abrirXmltv entrega o corpo da resposta para quem le`: smoke test de uma função pública
    que, sem ele, ficaria com zero cobertura.
- **YAGNI**: não criei nenhum auxiliar de JSON novo (reusei `JsonObject.long/int/texto` da
  T6, como o brief pediu); não criei builder, interface ou classe de configuração para o
  cliente HTTP; não adicionei parâmetro de timeout configurável no construtor de `XtreamApi`
  (o brief fixa a assinatura do construtor com só `cliente` e `cred` — os timeouts de 20s/120s
  são comportamento fixo da classe, não configuração externa).
- **Qualidade**: `executar()` centraliza requisição + timeout + detecção de redirecionamento
  + status HTTP num único ponto, usado por `autenticar`, `lerLista` e `abrirXmltv` — mudar a
  regra de erro de rede no futuro é mudança em um só lugar.
- **Risco que não decidi sozinho**: a autoria do commit. O brief dizia "repositório git sem
  `user.name`/`user.email` local" e instruía setar `GIT_AUTHOR_NAME`/`EMAIL` via variável de
  ambiente. **Conferi antes de commitar** (`git config user.name`/`user.email`) e isso não é
  verdade: o repositório **já tem** config local (`Marcos Ronaldo` /
  `marcossilva@rochamarinho.adv.br`). Pela regra global do usuário ("Autoria dos commits"),
  esse e-mail é citado literalmente como um que **já identifica o usuário** e que não deve
  ser trocado — só troco quando não há config local ou quando o e-mail é um dos genéricos da
  empresa (`nit@rms.adv.br`/`financeiroti@rms.adv.br`), o que não é o caso aqui. Commitei
  então com a autoria local, sem setar as variáveis de ambiente que o brief pedia. Acho que é
  a leitura certa da regra, mas registro a divergência do brief aqui para o controlador
  confirmar se concorda.

## Arquivos alterados

- `app/build.gradle.kts` (dependências `okhttp`/`okhttp-mockwebserver` adicionadas)
- `app/src/main/java/io/github/pagmovel/iptvplayer/xtream/XtreamApi.kt` (novo)
- `app/src/main/java/io/github/pagmovel/iptvplayer/xtream/FalhaXtream.kt` (novo)
- `app/src/test/java/io/github/pagmovel/iptvplayer/xtream/XtreamApiTest.kt` (novo)
- `.superpowers/sdd/2026-11-10-iptv-player-free-plano-1-base/progress.md` (entrada de
  despacho da T5, já existente antes de eu começar — incluída no commit pelo mesmo motivo da
  T6: o projeto versiona tudo em `.superpowers/`)

Commit: `ccf65d2` — "feat: cliente da API Xtream com leitura em fluxo". Sem push, sem tag.

## Pendências que não são minhas de resolver agora

- O bloqueio de 429 do Maven Central no artefato do Robolectric (achado pela T6, confirmado
  de novo aqui) ainda está ativo e vai continuar afetando qualquer tarefa que rode
  `testDebugUnitTest` oficial (T7, T8, T9, T10, T11) até liberar (~2026-11-11 18:48, por
  estimativa da T6). O caminho alternativo com `java -cp` continua funcionando.
- `fwcd.kotlin-0.2.36.vsix` solto na raiz do repositório, não rastreado — já existia antes de
  eu começar, sem relação com a T5. Não toquei nele.
