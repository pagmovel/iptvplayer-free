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
        // Espelho oficial do Google para o Maven Central, como repositório extra: o
        // repo.maven.apache.org está devolvendo HTTP 429 (limite de requisições) para o
        // artefato do Robolectric usado pelos testes (achado real da T9, confirmado com
        // curl -I nos dois hosts em 2026-11-10). O Gradle tenta este depois do mavenCentral()
        // quando o download falha.
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2") }
    }
}

rootProject.name = "iptv-player-free"
include(":app")
