plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.pagmovel.iptvplayer"

    // 37.2 é a plataforma estável mais recente disponível na imagem cimg/android:2026.10
    // (confirmado em .superpowers/sdd/.../task-1-report.md: source.properties de android-37.2
    // tem PreviewSdkInt=0 e CodeName vazio, ou seja, não é preview).
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "io.github.pagmovel.iptvplayer"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    // Built-in Kotlin (AGP 9+) deriva o jvmTarget do Kotlin a partir daqui; não existe mais
    // o bloco "kotlinOptions" do plugin org.jetbrains.kotlin.android (verificado rodando
    // o build real: "Unresolved reference 'kotlinOptions'").
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // O MavenArtifactFetcher do Robolectric baixa os jars android-all/android-all-instrumented
    // por conta própria, direto de repo1.maven.org — não passa pela resolução de dependências
    // do Gradle, então o exclusiveContent de settings.gradle.kts não alcança esse download, e
    // esse host também devolve HTTP 429 (achado real: T10 travou nisso mesmo depois do fix da
    // T9). Robolectric suporta oficialmente redirecionar esse fetcher para outro repositório
    // via system property (robolectric.org/blog/2023/11/11/improving-android-all-downloading),
    // sem precisar de usuário/senha para um espelho público.
    testOptions {
        unitTests.all {
            it.systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2")
            it.systemProperty("robolectric.dependency.repo.id", "google-mirror")
        }
    }
}

// Esquema do Room versionado no repositório, para dar suporte a migrações nas próximas
// versões do banco (ver app/src/main/.../dados/BancoApp.kt).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    // TestListenableWorkerBuilder (T11, CargaWorkerTest) — testa o CoroutineWorker sem
    // precisar de um WorkManager real rodando.
    testImplementation(libs.androidx.work.testing)
    // XmlPullParserFactory não roda fora do Android/Robolectric; kxml2 fornece a
    // implementação org.xmlpull.v1 para o teste JVM puro de LeitorXmltvTest (T8).
    testImplementation(libs.kxml2)
}
