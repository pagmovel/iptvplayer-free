pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Simplesmente declarar o espelho como repositório extra não bastou (achado real da
        // T9 em 2026-11-10): o Gradle resolve o POM pelo mavenCentral() e não tenta o próximo
        // repositório para o binário do mesmo módulo. exclusiveContent força esse módulo
        // específico — bloqueado com HTTP 429 (Cloudflare) em repo.maven.apache.org — a vir
        // só do espelho oficial do Google, sem passar pelo Maven Central.
        exclusiveContent {
            forRepository {
                maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2") }
            }
            filter {
                includeModule("org.robolectric", "nativeruntime-dist-compat")
            }
        }
    }
}

rootProject.name = "iptv-player-free"
include(":app")
