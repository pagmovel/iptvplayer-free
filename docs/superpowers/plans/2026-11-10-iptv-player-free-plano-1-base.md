# IPTV Player Free — Plano 1: base, build e carga de dados

> **Para agentes:** SUB-SKILL OBRIGATÓRIA: use superpowers:subagent-driven-development
> (recomendado) ou superpowers:executing-plans para executar este plano tarefa por tarefa. Os
> passos usam checkbox (`- [ ]`).
>
> **Autorizado pelo dono em 2026-11-10** após revisão. Execução em andamento via
> subagent-driven-development; ledger em
> `.superpowers/sdd/2026-11-10-iptv-player-free-plano-1-base/progress.md`.

**Objetivo:** um APK assinado, publicado nas Releases do GitHub e atualizável por cima, que
cadastra uma conta Xtream (inclusive colando o link do provedor), baixa canais, filmes, séries e
programação para um banco local sem travar, e mostra um resumo do que foi carregado.

**Arquitetura:** app Android de módulo único, organizado em pacotes por responsabilidade
(`conta`, `xtream`, `programacao`, `dados`, `carga`, `ui`). A API Xtream e o XMLTV são lidos **em
fluxo** e gravados em lotes num banco Room. A troca do catálogo acontece numa transação, então a
interface só enxerga o catálogo antigo inteiro ou o novo inteiro. A carga roda num `CoroutineWorker`
do WorkManager. As senhas são cifradas com uma chave AES-GCM do Android Keystore.

**Tecnologia:** Kotlin, Jetpack Compose (Material 3), Navigation Compose, Room com FTS4,
WorkManager, OkHttp, kotlinx.serialization (JSON), XmlPullParser (do Android; kxml2 nos testes),
JUnit4, Robolectric, MockWebServer, kotlinx-coroutines-test. Player Media3 só a partir do Plano 2.

**Design:** [docs/superpowers/specs/2026-11-10-iptv-player-free-design.md](../specs/2026-11-10-iptv-player-free-design.md).
Leia o design inteiro antes de começar, principalmente a §3.1 (dados reais do provedor).

## Mapa dos planos

O design tem 6 fases. Elas viram 4 planos, e cada plano entrega um APK que funciona sozinho. Cada
plano seguinte é escrito **depois** que o anterior estiver validado pelo dono no celular, para
aproveitar o que se aprendeu.

| Plano | Fases do design | Entrega | Estado |
|---|---|---|---|
| **1 · Base** (este) | 0 e 1 | build, assinatura, Releases, contas, carga do catálogo e da programação | `pendente` |
| 2 · TV ao vivo | 2 | tela dividida, player, canais por categoria, aba Programação, tela cheia | não escrito |
| 3 · Filmes, séries, Início e busca | 3 e 4 | vitrine, detalhes, temporadas, progresso, busca única, favoritos | não escrito |
| 4 · Distribuição | 5 | aviso de versão nova, README de instalação para família e amigos | não escrito |

## Acompanhamento das tarefas

Estados: `pendente` → `concluída` (código pronto) → `testada` (verificação rodou) → `validada`
(o dono confirmou). Atualize esta tabela a cada avanço, não só no fim.

| Fase | Tarefa | Estado |
|---|---|---|
| 0 | T1 · Esqueleto Gradle e testes no Docker | `testada` |
| 0 | T2 · Workflow do GitHub Actions (CI e Release assinada) | `testada` (`testes` e `release` passaram de verdade no Actions — run `43232261797`, tag `v0.0.3`) |
| 0 | T3 · Chave de assinatura, Secrets e primeira Release | `testada` (Release `v0.0.3` publicada de verdade; falta a validação do dono — passo 6) |
| 1 | T4 · Leitura do link do provedor | `testada` |
| 1 | T5 · Cliente da API Xtream | `testada` |
| 1 | T6 · Mapeamento do JSON Xtream para entidades | `testada` |
| 1 | T7 · Banco local (Room + FTS) | `testada` |
| 1 | T8 · Leitor de XMLTV | `testada` |
| 1 | T9 · Carga do catálogo (orquestração) | `testada` |
| 1 | T10 · Contas: cofre de senha e repositório | `testada` |
| 1 | T11 · Telas de contas e resumo, worker e Release v0.1.0 | `testada` (passos 1–6 e 9; passos 7–8 aguardam o dono) |

## Restrições globais

- Nome exibido: **IPTV Player Free**. Pacote: `io.github.pagmovel.iptvplayer` *(decisão do
  plano)*.
- `minSdk = 26` (Android 8). `targetSdk` e `compileSdk`: a API estável mais recente disponível
  na imagem Docker.
- Interface, comentários, commits e docs em **pt-BR**.
- **Repositório público:** `listas.txt` e `temp/` nunca entram no git (já estão no
  `.gitignore`). Os testes versionados usam **só dados sintéticos**. Nunca copie trechos de
  `temp/amostras/` para os testes: copie só o *formato*, com nomes e valores inventados.
- Tráfego `http` sem TLS precisa ser permitido (`android:usesCleartextTraffic="true"`), porque o
  provedor só serve por `http`.
- Nada de lista inteira em memória. JSON lido com `Json.decodeToSequence` sobre o `InputStream`,
  XMLTV com `XmlPullParser`, gravação em lotes de **500**.
- Programação guardada só na janela **de ontem 00:00 até depois de amanhã 23:59:59**, no fuso do
  aparelho.
- Atualização automática do catálogo ao abrir o app quando a última carga bem-sucedida tiver
  mais de **12 horas** *(decisão do plano)*, além do botão manual.
- **Verificação local (sem instalar nada no Windows):** `./scripts/gradle.ps1 <tarefas>` roda o
  Gradle dentro de `cimg/android:2026.10` (imagem conferida em 2026-11-10). Criado na T1.
- Versões de bibliotecas: a última estável no momento da T1, registrada em
  `gradle/libs.versions.toml`. Não fixe versões de memória: confira na documentação oficial e
  anote a fonte no commit.
- **Commits:** o repositório não tem `user.name`/`user.email` local. Pelas regras do dono, use
  `GIT_AUTHOR_NAME`/`GIT_COMMITTER_NAME="Marcos Ronaldo Almeida Silva"` e
  `GIT_AUTHOR_EMAIL`/`GIT_COMMITTER_EMAIL="marcossilva@rms.adv.br"` como variáveis de ambiente,
  sem mexer no `git config`. Mensagem em pt-BR, terminando com
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **`git push`, criação de tag e qualquer ação no GitHub exigem o "sim" do dono no momento.**
  Push publica num repositório público.
- Leitura de arquivos e saídas pelo plugin context-mode (`ctx_execute`/`ctx_execute_file`),
  imprimindo só o necessário. `Read` nativo só no arquivo que vai receber `Edit` em seguida.
  Todo subagente recebe esta instrução escrita no próprio prompt.

## Foco de revisão

Situações que o design implica e que um usuário real vai encontrar, com a tarefa que as testa:

1. **Campos do JSON Xtream com tipo inconsistente** (número vindo como `"123"`, `null`,
   `rating: ""`, `category_id: null`, `stream_icon` vazio). O item é importado com o campo vazio
   e a carga não quebra. Testado na T6.
2. **Senha errada, conta vencida ou servidor fora do ar no meio da carga.** O app mostra uma
   mensagem clara, e o catálogo anterior continua intacto. Testado na T5 (`auth = 0`, HTTP 5xx,
   timeout) e na T9 (falha no meio mantém os dados antigos).
3. **Link colado do jeito que o provedor manda**, com emoji, asterisco, texto antes, espaço,
   quebra de linha e porta `:80`. Os campos são preenchidos corretamente. Testado na T4.
4. **XMLTV com fuso `+0000` e outros fusos, programa fora da janela, título ausente, entidades
   `&amp;`, ids de canal repetidos.** Fica só o que cai na janela, com horário correto. Testado
   na T8.
5. **Busca sem acento e sem diferenciar maiúsculas** ("sao paulo" acha "São Paulo"). Testado na
   T7, porque a tabela FTS e o tokenizador são definidos lá, e trocá-los depois exige migração.

---

## Fase 0 · Esqueleto e build

### T1: Esqueleto Gradle e testes no Docker

**Arquivos:**
- Criar: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`,
  `gradle/libs.versions.toml`, wrapper do Gradle (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`)
- Criar: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`,
  `app/src/main/java/io/github/pagmovel/iptvplayer/ui/MainActivity.kt`,
  `app/src/main/res/values/strings.xml` (`app_name` = `IPTV Player Free`), ícone adaptativo
  simples (tema escuro, sem lembrar outro app)
- Criar: `scripts/gradle.ps1`
- Teste: `app/src/test/java/io/github/pagmovel/iptvplayer/EsqueletoTest.kt`

**Interfaces:**
- Produz: `./scripts/gradle.ps1 <tarefas...>`, usado por todas as tarefas para verificar.
- Produz: catálogo de versões com aliases `androidx-compose-bom`, `androidx-room-*`,
  `androidx-work-runtime`, `okhttp`, `okhttp-mockwebserver`, `kotlinx-serialization-json`,
  `kotlinx-coroutines-test`, `robolectric`, `kxml2`, `junit`.

- [x] **Passo 1: Conferir a imagem.** Rode
  `docker run --rm cimg/android:2026.10 bash -lc "java -version; sdkmanager --list_installed | head -40; gradle --version; id"`.
  Anote no commit o JDK, as plataformas `platforms;android-NN` instaladas (`compileSdk` = maior
  NN), se há `gradle` no PATH, e o usuário/home (esperado `circleci`, `/home/circleci`
  **[A VERIFICAR]**).
- [x] **Passo 2: Criar `scripts/gradle.ps1`.** O script recebe as tarefas como argumentos e roda
  `docker run --rm -v "${PWD}:/projeto" -v iptv-gradle-cache:<home>/.gradle -w /projeto cimg/android:2026.10 ./gradlew @args --no-daemon`,
  devolvendo o código de saída do Docker. `<home>` vem do Passo 1.
- [x] **Passo 3: Gerar o wrapper** com a última versão estável do Gradle compatível com o AGP
  escolhido, usando o `gradle` da imagem. Se a imagem não tiver `gradle`, use `gradle:jdk17`
  só para este comando.
- [x] **Passo 4: Escrever o teste que falha.**

```kotlin
class EsqueletoTest {
    @Test fun `nome do app e pacote estao corretos`() {
        assertEquals("io.github.pagmovel.iptvplayer", BuildConfig.APPLICATION_ID)
    }
}
```

- [x] **Passo 5: Rodar e ver falhar.** `./scripts/gradle.ps1 testDebugUnitTest`. Esperado:
  falha de compilação (sem `app/build.gradle.kts`/`BuildConfig`).
- [x] **Passo 6: Implementar o esqueleto.** Use AGP, Kotlin, plugin Compose do Kotlin e plugin
  de serialização, com `buildFeatures { compose = true; buildConfig = true }`, `minSdk = 26`,
  `usesCleartextTraffic="true"` e permissão `INTERNET`. A `MainActivity` mostra só "IPTV Player
  Free" num tema escuro do Material 3. Declare no catálogo todas as dependências do plano, mesmo
  as que só serão usadas depois, para as próximas tarefas não mexerem em versões.
- [x] **Passo 7: Rodar e ver passar.** `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`.
  Esperado: `BUILD SUCCESSFUL` e `app/build/outputs/apk/debug/app-debug.apk` criado.
- [x] **Passo 8: Commit.** `build: esqueleto do app Android e build via Docker`.

### T2: Workflow do GitHub Actions (CI e Release assinada)

**Arquivos:**
- Criar: `.github/workflows/build.yml`
- Modificar: `app/build.gradle.kts` (assinatura e versão)

**Interfaces:**
- Consome: Secrets `ASSINATURA_KEYSTORE_BASE64`, `ASSINATURA_KEYSTORE_SENHA`,
  `ASSINATURA_ALIAS`, `ASSINATURA_ALIAS_SENHA` (cadastrados na T3).
- Produz: em todo push e pull request, `testDebugUnitTest` + `assembleDebug`. Em tag `v*`, os
  testes, `assembleRelease` assinado e uma Release do GitHub com o anexo
  `iptv-player-free-<versão>.apk`.

- [x] **Passo 1: Assinatura e versão no Gradle.** `signingConfigs.release` lê as 4 variáveis de
  ambiente acima; o keystore é decodificado do base64 para `app/assinatura.jks` pelo workflow.
  Sem as variáveis, o `release` não assina, para o build local não quebrar. `versionName` = tag
  sem o `v` (variável `VERSAO_APP`; padrão `0.0.0-dev`). `versionCode` = `GITHUB_RUN_NUMBER`
  (padrão `1`). Adicione `app/assinatura.jks` e `*.jks` ao `.gitignore`.
  - **Desvio do passo (anotado, não é suposição):** em vez de `actions/setup-java` + SDK do
    runner, o workflow roda os dois jobs dentro do mesmo container `cimg/android:2026.10` do
    `scripts/gradle.ps1`, via `docker run` — garante a mesma versão de SDK/Gradle/Kotlin já
    validada nesta sessão em vez do que o `ubuntu-latest` tiver instalado. Confirmado antes
    (`docker run cimg/android:2026.10 bash -lc "which gh"`) que a imagem não tem `gh`, então só a
    montagem do APK roda no container; `gh release create` roda fora dele, direto no runner.
- [x] **Passo 2: Workflow.** Os jobs `testes` (sempre) e `release` (`if:
  startsWith(github.ref, 'refs/tags/v')`, depende de `testes`) criam a Release com
  `gh release create "$GITHUB_REF_NAME" <apk> --generate-notes`. `permissions: contents: write`
  só no job de release. Keystore nunca sobe como artefato.
- [x] **Passo 3: Verificar localmente** que o build sem as variáveis continua funcionando:
  `./scripts/gradle.ps1 testDebugUnitTest assembleDebug assembleRelease`. Resultado:
  `BUILD SUCCESSFUL in 1m 2s`, com `app/build/outputs/apk/release/app-release-unsigned.apk`
  gerado (9.378.118 bytes).
- [x] **Passo 4: Commit.** `0e5021d` — `feat: CI/CD com build/testes automáticos e assinatura
  condicional (T2)`.

### T3: Chave de assinatura, Secrets e primeira Release

Tarefa com ações do dono. Cada push e cada tag precisam do "sim" dele na hora.

**Arquivos:**
- Criar (fora do git): `temp/assinatura/iptv-player-free.jks`,
  `temp/assinatura/valores-secrets.txt`

- [x] **Passo 1: Gerar a chave.** **Desvio do passo (achado real, não suposição):** o comando
  original com `keytool.exe` do JDK 8 local gerou uma chave **incompatível** com o OpenJDK 21
  de dentro do `cimg/android:2026.10` (o que assina no CI) — a tag `v0.0.1` falhou de verdade
  no Actions com `KeytoolException: ... Password is not ASCII`, mesmo a senha sendo só
  hexadecimal. Corrigido gerando a chave **dentro do mesmo container** (comando e pegadinha
  completos em `.claude/skills/iptv-build-assinatura/SKILL.md`). Chave real regenerada e
  verificada localmente (`BUILD SUCCESSFUL` + `apksigner verify` confirmando o certificado).
  Senha aleatória forte; em PKCS12 a senha da chave é igual à do keystore. `valores-secrets.txt`
  atualizado com os 4 valores novos.
- [x] **Passo 2: Conferir que nada vazou.** `git status --short` não mostra nada de `temp/`, e
  `git check-ignore -v temp/assinatura/iptv-player-free.jks` mostra a regra `temp/`.
- [x] **Passo 3: Guiar o dono no cadastro dos Secrets.** Os 4 Secrets foram cadastrados com a
  chave antiga (incompatível) antes da correção do passo 1, e **atualizados pelo dono** com os
  valores novos em 2026-11-12 (confirmado pelo "Last updated" de cada um na página de Secrets:
  os 4 mostravam "now"/poucos minutos antes do teste da tag `v0.0.3`).
- [x] **Passo 4: Com o "sim" do dono,** faça `git push -u origin main`. Esperado: job `testes`
  verde no Actions. Confirmado: run #1 (`43083373024`), `testes` = `success`, `release` =
  `skipped` (correto — só roda em tag).
- [x] **Passo 5: Com o "sim" do dono,** crie e envie a tag. **Histórico real das tentativas:**
  `v0.0.1` (run `43088431682`) falhou no job `release` pela chave incompatível do passo 1 —
  nenhuma Release publicada (404 em `/releases/tags/v0.0.1`). `v0.0.2` (run `43097855518`)
  ficou presa em `queued` por um outage real da plataforma GitHub (confirmado em
  `githubstatus.com`); mesmo depois do outage resolvido o run não saiu do estado e nem o
  "Cancel workflow" da UI funcionou — ficou órfã, sem Release. **`v0.0.3` (run
  `43232261797`) passou de verdade**: `testes` = `success`, `release` = `success`. Release
  publicada: https://github.com/pagmovel/iptvplayer-free/releases/tag/v0.0.3 com o anexo
  `iptv-player-free-0.0.3.apk` (9.390.406 bytes — mesmo tamanho verificado localmente com
  `apksigner` antes do cadastro dos Secrets).
- [ ] **Passo 6: Validação pelo dono:** instalar a `v0.0.3` no celular. Depois, mude o texto da
  tela para incluir a versão seguinte, publique outra tag (com "sim") e peça que ele instale
  **por cima**, sem desinstalar. Esperado: instala sem "conflito com pacote existente". Só então
  a Fase 0 vira `validada`.
- [x] **Passo 7: Documentar.** `.claude/skills/iptv-build-assinatura/SKILL.md` criada com o
  processo de chave e Secrets, o comando do Docker, e as pegadinhas (JDK incompatível, bind
  mount do Windows, logs do Actions exigindo auth). Commit a seguir.

---

## Fase 1 · Contas e carga de dados

Base dos pacotes: `app/src/main/java/io/github/pagmovel/iptvplayer/`; testes em
`app/src/test/java/io/github/pagmovel/iptvplayer/`. Todas as verificações rodam com
`./scripts/gradle.ps1 testDebugUnitTest --tests '<Classe>'`. O resultado esperado é a linha
`BUILD SUCCESSFUL`, ou a falha do teste novo no passo "ver falhar".

### T4: Leitura do link do provedor

**Arquivos:**
- Criar: `conta/LinkXtream.kt`
- Teste: `conta/LinkXtreamTest.kt`

**Interfaces:**
- Produz: `data class CredenciaisXtream(val servidor: String, val usuario: String, val senha: String)`.
  `servidor` = esquema + host + porta (se houver), sem barra final, ex. `http://exemplo.tv:80`.
- Produz: `fun lerLinkXtream(texto: String): CredenciaisXtream?`

- [x] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `extrai do link no formato que o provedor manda`() {
    val texto = "🟢 *Link Dns (M3U) :\n\n http://exemplo.tv:80/get.php?username=joao&password=s3nh4&type=m3u_plus&output=mpegts\n"
    assertEquals(CredenciaisXtream("http://exemplo.tv:80", "joao", "s3nh4"), lerLinkXtream(texto))
}
@Test fun `aceita link da player_api e sem porta`() {
    assertEquals(CredenciaisXtream("http://exemplo.tv", "a", "b"),
        lerLinkXtream("http://exemplo.tv/player_api.php?username=a&password=b"))
}
@Test fun `decodifica caracteres codificados na senha`() {
    assertEquals("a+b&c", lerLinkXtream("http://x.tv/get.php?username=u&password=a%2Bb%26c")!!.senha)
}
@Test fun `devolve null sem usuario ou senha ou sem link`() {
    assertNull(lerLinkXtream("http://x.tv/get.php?username=u"))
    assertNull(lerLinkXtream("texto qualquer"))
    assertNull(lerLinkXtream(""))
}
```

- [x] **Passo 2: Rodar e ver falhar** (`--tests '*LinkXtreamTest'`).
- [x] **Passo 3: Implementar `lerLinkXtream`.** Ache o primeiro `https?://\S+` no texto e
  interprete-o com `java.net.URI`. Decodifique os parâmetros com `URLDecoder` em UTF-8. Não use
  `android.net.Uri`, que não funciona em teste na JVM sem Robolectric.
- [x] **Passo 4: Rodar e ver passar.**
- [x] **Passo 5: Commit.** `feat: leitura de credenciais a partir do link do provedor`.

### T5: Cliente da API Xtream

**Arquivos:**
- Criar: `xtream/XtreamApi.kt`, `xtream/FalhaXtream.kt`
- Teste: `xtream/XtreamApiTest.kt` (MockWebServer)

**Interfaces:**
- Consome: `CredenciaisXtream` (T4).
- Produz: `class XtreamApi(private val cliente: OkHttpClient, private val cred: CredenciaisXtream)`, com:
  - `suspend fun autenticar(): InfoConta`, onde
    `data class InfoConta(val ativa: Boolean, val expiraEm: Long?, val maxConexoes: Int?)`
    (`expiraEm` em epoch segundos);
  - `suspend fun lerLista(acao: String, tamanhoLote: Int = 500, aoLote: suspend (List<JsonObject>) -> Unit): Int`,
    que devolve o total lido;
  - `suspend fun <T> abrirXmltv(ler: suspend (InputStream) -> T): T`;
  - `fun urlAoVivo(streamId: Long): String` → `"$servidor/live/$usuario/$senha/$streamId.ts"`;
    `fun urlFilme(streamId: Long, extensao: String)` → `/movie/...`;
    `fun urlEpisodio(id: Long, extensao: String)` → `/series/...`.
- Produz: `sealed class FalhaXtream : Exception`, com os casos `CredenciaisInvalidas`,
  `ContaInativa(status: String)`, `ServidorIndisponivel(causa: Throwable?)` e
  `RespostaInvalida(detalhe: String)`.

- [x] **Passo 1: Escrever os testes que falham.** Use JSON sintético no formato da §3.1.

```kotlin
@Test fun `autenticar le conta ativa`()            // {"user_info":{"auth":1,"status":"Active","exp_date":"1800000000","max_connections":"2"},...}
    // assertEquals(InfoConta(true, 1800000000, 2), api.autenticar()); path == "/player_api.php?username=u&password=p"
@Test fun `auth 0 vira CredenciaisInvalidas`()       // {"user_info":{"auth":0}}
@Test fun `status Expired vira ContaInativa`()       // assertEquals("Expired", (falha as ContaInativa).status)
@Test fun `HTTP 503 e timeout viram ServidorIndisponivel`()
@Test fun `resposta HTML vira RespostaInvalida`()    // corpo "<!doctype html>…", como o redirect do get.php
@Test fun `lerLista entrega em lotes sem passar do tamanho`() {
    // servidor devolve um array JSON com 1234 objetos {"stream_id":N,"name":"Canal N"}
    // lotes = mutableListOf<Int>(); total = api.lerLista("get_live_streams", 500) { lotes += it.size }
    // assertEquals(listOf(500, 500, 234), lotes); assertEquals(1234, total)
    // path termina em "&action=get_live_streams"
}
```

- [x] **Passo 2: Rodar e ver falhar.**
- [x] **Passo 3: Implementar.** Use OkHttp com `callTimeout` de 120 s para listas e 20 s para
  autenticar. Não siga redirecionamentos para outro host (`followRedirects(false)` no cliente da
  API): um 3xx vira `RespostaInvalida`. `lerLista` usa
  `Json.decodeToSequence<JsonObject>(corpo.byteStream(), DecodeSequenceMode.ARRAY_WRAPPED)`. Os
  números vêm como texto ou número: leia-os com os auxiliares da T6 (`JsonObject.long`/`int`).
  Implemente a T6 primeiro se precisar, ou declare esses auxiliares aqui e mova-os depois. Rode
  o I/O em `Dispatchers.IO`.
- [x] **Passo 4: Rodar e ver passar.**
- [x] **Passo 5: Commit.** `feat: cliente da API Xtream com leitura em fluxo`.

### T6: Mapeamento do JSON Xtream para entidades

**Arquivos:**
- Criar: `xtream/JsonCampos.kt`, `xtream/Mapeamento.kt`
- Teste: `xtream/MapeamentoTest.kt`

**Interfaces:**
- Consome: as entidades da T7 (`Categoria`, `Canal`, `Filme`, `Serie`). Escreva a T7 antes, ou
  defina primeiro só as `data class` (sem anotações Room) e complete na T7.
- Produz, em `JsonCampos.kt`: `fun JsonObject.texto(campo: String): String?` (vazio vira `null`),
  `fun JsonObject.long(campo: String): Long?`, `fun JsonObject.int(campo: String): Int?` e
  `fun JsonObject.decimal(campo: String): Double?`. Todos aceitam número, texto numérico, `null`,
  campo ausente e texto vazio.
- Produz, em `Mapeamento.kt`: `fun JsonObject.paraCategoria(contaId: Long, tipo: TipoConteudo): Categoria?`,
  `paraCanal(contaId)`, `paraFilme(contaId)` e `paraSerie(contaId)`. Cada um devolve `null` sem
  id ou nome.

- [x] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `numero como texto e como numero`() {
    assertEquals(42L, obj("""{"stream_id":"42"}""").long("stream_id"))
    assertEquals(42L, obj("""{"stream_id":42}""").long("stream_id"))
}
@Test fun `vazio nulo e ausente viram null`() {
    val o = obj("""{"rating":"","category_id":null}""")
    assertNull(o.decimal("rating")); assertNull(o.long("category_id")); assertNull(o.texto("nao_existe"))
}
@Test fun `canal sem categoria e sem icone ainda e importado`() {
    val c = obj("""{"stream_id":7,"name":"Canal Sete","category_id":null,"stream_icon":"","epg_channel_id":"sete.br","num":3}""").paraCanal(1)!!
    assertEquals(Canal(contaId = 1, streamId = 7, nome = "Canal Sete", categoriaId = null, icone = null, epgId = "sete.br", numero = 3), c)
}
@Test fun `filme le extensao ano e nota`()   // container_extension "mkv", year "2019", rating "7.5" → Filme(... extensao="mkv", ano=2019, nota=7.5)
@Test fun `serie le capa sinopse e ultima modificacao`()
@Test fun `item sem id ou sem nome devolve null`()
```

- [x] **Passo 2: Rodar e ver falhar.**
- [x] **Passo 3: Implementar** os auxiliares e os quatro mapeamentos. Os campos vêm da §3.1:
  canal (`stream_id`, `name`, `category_id`, `stream_icon`, `epg_channel_id`, `num`), filme
  (`stream_id`, `name`, `category_id`, `stream_icon`, `container_extension`, `year`, `rating`,
  `added`), série (`series_id`, `name`, `category_id`, `cover`, `plot`, `year`, `rating`,
  `last_modified`) e categoria (`category_id`, `category_name`).
- [x] **Passo 4: Rodar e ver passar.**
- [x] **Passo 5: Commit.** `feat: mapeamento tolerante do JSON Xtream`.

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

- [x] **Passo 1: Escrever os testes que falham.**

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

- [x] **Passo 2: Rodar e ver falhar.**
- [x] **Passo 3: Implementar.** Para o termo do FTS, separe por espaço, escape aspas e junte como
  `"palavra"*` com AND implícito, para a busca por prefixo funcionar. Ligue a tabela FTS à
  entidade por `rowid`. Ative `ksp` com `room.schemaLocation`.
- [x] **Passo 4: Rodar e ver passar.**
- [x] **Passo 5: Commit.** `feat: banco local com busca FTS sem acento`.

### T8: Leitor de XMLTV

**Arquivos:**
- Criar: `programacao/LeitorXmltv.kt`
- Teste: `programacao/LeitorXmltvTest.kt` (JVM com `kxml2` como `testImplementation`)

**Interfaces:**
- Consome: `Programa` (T7).
- Produz: `fun lerXmltv(entrada: InputStream, contaId: Long, janela: LongRange, tamanhoLote: Int = 500, aoLote: (List<Programa>) -> Unit): Int`,
  que devolve o total aceito. `janela` em epoch ms.
- Produz: `fun janelaProgramacao(agora: ZonedDateTime): LongRange`, de ontem 00:00 até depois de
  amanhã 23:59:59.999, no fuso de `agora`.

- [x] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `converte horario com fuso`() {
    // <programme start="20261110030000 +0000" stop="20261110050000 +0000" channel="a.br"><title>Jornal</title></programme>
    // assertEquals(Instant.parse("2026-11-10T03:00:00Z").toEpochMilli(), p.inicio)
    // idem com "+0300" → 2026-11-10T00:00:00Z
}
@Test fun `ignora programa fora da janela`()
@Test fun `aceita programa que comeca antes e termina dentro da janela`()
@Test fun `titulo ausente vira Sem titulo e entidades sao decodificadas`()  // "A&amp;E" → "A&E"
@Test fun `ignora tags channel e programa sem channel ou horario invalido`()
@Test fun `entrega em lotes`()   // 1234 programas na janela → lotes 500, 500, 234
@Test fun `janela vai de ontem 0h a depois de amanha 23h59`() {
    val j = janelaProgramacao(ZonedDateTime.parse("2026-11-10T15:00-03:00[America/Sao_Paulo]"))
    assertEquals(ZonedDateTime.parse("2026-11-09T00:00-03:00[America/Sao_Paulo]").toInstant().toEpochMilli(), j.first)
    assertEquals(ZonedDateTime.parse("2026-11-13T00:00-03:00[America/Sao_Paulo]").toInstant().toEpochMilli() - 1, j.last)
}
```

- [x] **Passo 2: Rodar e ver falhar.**
- [x] **Passo 3: Implementar** com `XmlPullParserFactory.newInstance().newPullParser()`. Leia
  `start`/`stop` pelo padrão `yyyyMMddHHmmss Z` com `DateTimeFormatter`, que funciona no minSdk 26
  sem desugaring. Ignore os atributos `start_timestamp`, porque nem todo XMLTV os tem. Use só os
  filhos `title` e `desc`.
- [x] **Passo 4: Rodar e ver passar.**
- [x] **Passo 5: Commit.** `feat: leitor de XMLTV em fluxo com janela de 4 dias`.

### T9: Carga do catálogo (orquestração)

**Arquivos:**
- Criar: `carga/CargaCatalogo.kt`
- Teste: `carga/CargaCatalogoTest.kt` (Robolectric, Room em memória, MockWebServer)

**Interfaces:**
- Consome: `XtreamApi.autenticar/lerLista/abrirXmltv` (T5), mapeamentos (T6), `CatalogoDao` e
  `BancoApp.withTransaction` (T7), `lerXmltv`/`janelaProgramacao` (T8).
- Produz: `class CargaCatalogo(private val api: XtreamApi, private val banco: BancoApp, private val contaId: Long, private val agora: () -> ZonedDateTime = { ZonedDateTime.now() })`, com
  `suspend fun executar(progresso: (EtapaCarga) -> Unit = {}): Contagem`.
- Produz: `enum class EtapaCarga { AUTENTICANDO, CATEGORIAS, CANAIS, FILMES, SERIES, PROGRAMACAO, CONCLUIDA }`.
- Ordem: autenticar; numa transação, apagar o catálogo da conta e inserir categorias (os 3
  tipos), canais, filmes e séries em lotes; depois, noutra transação, trocar a programação.
  Falha na programação **não** desfaz o catálogo, porque canal sem programação é aceito pelo
  design.

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `carga completa grava tudo e informa as etapas`()  // contagem == Contagem(3, 2, 1, 4); etapas na ordem do enum
@Test fun `falha nos filmes mantem o catalogo anterior inteiro`() {
    // 1ª carga OK (3 canais); 2ª carga: get_vod_streams devolve 503
    // assertFailsWith<FalhaXtream.ServidorIndisponivel> { carga.executar() }
    // assertEquals(3, dao.contagem(1).canais)  // nada pela metade
}
@Test fun `senha errada nao apaga nada`()
@Test fun `falha so na programacao mantem catalogo novo e programacao antiga`()
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar `executar`.**
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: carga do catálogo em transação, sem perder dados em falha`.

### T10: Contas: cofre de senha e repositório

**Arquivos:**
- Criar: `conta/Cofre.kt`, `conta/ContaDao.kt`, `conta/ContasRepositorio.kt`
- Teste: `conta/ContasRepositorioTest.kt` (Robolectric, com `Cofre` falso)

**Interfaces:**
- Produz: `interface Cofre { fun cifrar(texto: String): ByteArray; fun decifrar(dados: ByteArray): String }`
  e `class CofreKeystore : Cofre` (AES/GCM/NoPadding, chave `iptv-player-free-senhas` no
  `AndroidKeyStore`, IV de 12 bytes prefixado ao resultado). A interface existe porque o
  Robolectric não emula o `AndroidKeyStore`. O falso dos testes é a segunda implementação.
- Produz: `ContaDao` com `inserir`, `atualizar`, `apagar`, `listar(): Flow<List<Conta>>`,
  `ativa(): Conta?` e `marcarCarga(id, quando)`.
- Produz: `class ContasRepositorio(private val banco: BancoApp, private val cofre: Cofre, private val criarApi: (CredenciaisXtream) -> XtreamApi)`, com:
  - `suspend fun testarESalvar(nome: String, cred: CredenciaisXtream): Result<Conta>`, que chama
    `autenticar()` e só salva se der certo. A primeira conta salva fica ativa;
  - `suspend fun ativar(id: Long)`, que deixa exatamente uma ativa;
  - `suspend fun apagar(id: Long)`, que apaga catálogo e programação da conta;
  - `suspend fun credenciais(conta: Conta): CredenciaisXtream`;
  - `suspend fun precisaAtualizar(conta: Conta, agoraMs: Long): Boolean`, verdadeiro se
    `ultimaCargaEm` for `null` ou tiver mais de 12 h.

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `senha nunca e gravada em texto puro`()     // bytes de conta.senhaCifrada não contêm "s3nh4"
@Test fun `credencial invalida nao salva conta`()       // MockWebServer auth 0 → Result.failure; listar() vazio
@Test fun `primeira conta fica ativa e ativar troca`()  // depois de ativar(b): só b.ativa
@Test fun `apagar conta apaga o catalogo dela`()
@Test fun `precisa atualizar depois de 12 horas`()       // null → true; 11h59 → false; 12h01 → true
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar.** Coloque o `ContaDao` no `BancoApp` (ainda na versão 1, porque
  nada foi publicado com dados).
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: contas com senha cifrada e troca de conta ativa`.

### T11: Telas de contas e resumo, worker e Release v0.1.0

**Arquivos:**
- Criar: `carga/CargaWorker.kt`, `ui/Navegacao.kt`, `ui/contas/ContasTela.kt`,
  `ui/contas/ContasViewModel.kt`, `ui/resumo/ResumoTela.kt`, `ui/resumo/ResumoViewModel.kt`,
  `ui/Tema.kt`, `AppIptv.kt` (Application, montagem manual das dependências, sem framework de DI)
- Modificar: `ui/MainActivity.kt`, `AndroidManifest.xml`
- Teste: `carga/CargaWorkerTest.kt` (`TestListenableWorkerBuilder`, Robolectric)

**Interfaces:**
- Consome: `ContasRepositorio` (T10), `CargaCatalogo` (T9), `lerLinkXtream` (T4).
- Produz: `CargaWorker` (`CoroutineWorker`), com entrada `contaId`, progresso `etapa` (nome do
  `EtapaCarga`) e falha com `mensagem` em pt-BR. Ele é enfileirado como trabalho único por conta
  (`ExistingWorkPolicy.KEEP`) e exige rede conectada. No sucesso, chama `marcarCarga`.

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `worker grava o catalogo e marca a carga`()      // Result.success(); ultimaCargaEm != null
@Test fun `worker traduz falha em mensagem`()               // auth 0 → Result.failure com mensagem "Usuário ou senha incorretos"
```

  Mensagens *(decisão do plano)*: `CredenciaisInvalidas` → "Usuário ou senha incorretos";
  `ContaInativa` → "Conta do provedor inativa ou vencida (<status>)"; `ServidorIndisponivel` →
  "Servidor não respondeu. Tente outro endereço do seu provedor."; `RespostaInvalida` → "O
  servidor respondeu algo inesperado."
- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar o worker.**
- [ ] **Passo 4: Implementar as telas.**
  - **Contas:** lista das contas salvas, com a ativa marcada. Botão "Adicionar conta" abre um
    formulário com Nome, Servidor, Usuário e Senha, e com o botão **"Colar link do provedor"**,
    que lê a área de transferência com `lerLinkXtream` e preenche os 3 campos; se o link não
    servir, mostra "Link não reconhecido". "Salvar" chama `testarESalvar` com um indicador de
    carregamento e mostra o erro traduzido. Ações por conta: ativar, editar (trocar servidor) e
    apagar, com confirmação.
  - **Resumo** (tela inicial provisória, que o Plano 3 troca pela vitrine): nome da conta ativa;
    as contagens de canais, filmes, séries e programas; a etapa atual da carga; a data da última
    carga; e o botão "Atualizar agora". Sem conta, abre direto o formulário.
  - Ao abrir o app, se `precisaAtualizar` for verdadeiro, enfileira o `CargaWorker`.
  - Use o tema escuro do Material 3, coerente com os esboços (fundo `#0f1115`, destaque
    `#4fc3f7`), com textos de pelo menos 14 sp.
- [ ] **Passo 5: Rodar tudo.** `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`. Esperado:
  `BUILD SUCCESSFUL`.
- [ ] **Passo 6: Commit.** `feat: telas de contas e resumo com carga em segundo plano`.
- [ ] **Passo 7: Com o "sim" do dono,** faça push e publique a tag `v0.1.0`.
- [ ] **Passo 8: Validação pelo dono no celular,** atualizando por cima da `v0.0.2`:
  1. Colar um dos links de `listas.txt`: os campos são preenchidos.
  2. Salvar: a conta é aceita.
  3. Esperar a carga: ela termina sem travar a tela, e o resumo mostra números próximos de 946
     canais, 16.122 filmes e 8.714 séries (§3.1; o provedor muda isso com o tempo).
  4. Fechar e abrir: nada recarrega antes de 12 h.
  5. "Atualizar agora" com o Wi-Fi desligado: aparece mensagem, e os números continuam.
- [ ] **Passo 9: Documentar.** Crie `.claude/skills/iptv-xtream-carga/SKILL.md` com o formato
  real da API, as pegadinhas (temporadas vazias, `get.php` redirecionando, tipos inconsistentes,
  ids de EPG repetidos), os acertos e como validar. Atualize as tabelas de acompanhamento deste
  plano e a §6 do design. Commit `docs: skill da API Xtream e da carga`.
