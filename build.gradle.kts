// Build raiz: só declara os plugins (versões centralizadas no catálogo), sem aplicá-los aqui.
// Cada módulo aplica o que precisa via alias(libs.plugins....).
//
// Sem plugin "kotlin-android": a partir do AGP 9.0 o suporte a Kotlin já vem embutido
// (built-in Kotlin); aplicar org.jetbrains.kotlin.android dá erro de build
// (https://kotl.in/gradle/agp-built-in-kotlin, verificado rodando o build real).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
