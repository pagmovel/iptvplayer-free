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

- [ ] **Passo 1: Conferir a imagem.** Rode
  `docker run --rm cimg/android:2026.10 bash -lc "java -version; sdkmanager --list_installed | head -40; gradle --version; id"`.
  Anote no commit o JDK, as plataformas `platforms;android-NN` instaladas (`compileSdk` = maior
  NN), se há `gradle` no PATH, e o usuário/home (esperado `circleci`, `/home/circleci`
  **[A VERIFICAR]**).
- [ ] **Passo 2: Criar `scripts/gradle.ps1`.** O script recebe as tarefas como argumentos e roda
  `docker run --rm -v "${PWD}:/projeto" -v iptv-gradle-cache:<home>/.gradle -w /projeto cimg/android:2026.10 ./gradlew @args --no-daemon`,
  devolvendo o código de saída do Docker. `<home>` vem do Passo 1.
- [ ] **Passo 3: Gerar o wrapper** com a última versão estável do Gradle compatível com o AGP
  escolhido, usando o `gradle` da imagem. Se a imagem não tiver `gradle`, use `gradle:jdk17`
  só para este comando.
- [ ] **Passo 4: Escrever o teste que falha.**

```kotlin
class EsqueletoTest {
    @Test fun `nome do app e pacote estao corretos`() {
        assertEquals("io.github.pagmovel.iptvplayer", BuildConfig.APPLICATION_ID)
    }
}
```

- [ ] **Passo 5: Rodar e ver falhar.** `./scripts/gradle.ps1 testDebugUnitTest`. Esperado:
  falha de compilação (sem `app/build.gradle.kts`/`BuildConfig`).
- [ ] **Passo 6: Implementar o esqueleto.** Use AGP, Kotlin, plugin Compose do Kotlin e plugin
  de serialização, com `buildFeatures { compose = true; buildConfig = true }`, `minSdk = 26`,
  `usesCleartextTraffic="true"` e permissão `INTERNET`. A `MainActivity` mostra só "IPTV Player
  Free" num tema escuro do Material 3. Declare no catálogo todas as dependências do plano, mesmo
  as que só serão usadas depois, para as próximas tarefas não mexerem em versões.
- [ ] **Passo 7: Rodar e ver passar.** `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`.
  Esperado: `BUILD SUCCESSFUL` e `app/build/outputs/apk/debug/app-debug.apk` criado.
- [ ] **Passo 8: Commit.** `build: esqueleto do app Android e build via Docker`.

