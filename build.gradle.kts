// Root build file - Plugin management and shared tasks
plugins {
    kotlin("jvm") version "2.2.0"
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.secrets) apply false
}

// Shared root-level tasks
tasks.register("zipExtension") {
    group = "build"
    description = "Zips the extension for browser installation"
    doLast {
        println("Zipping JS files for Web Extension...")
    }
}


dependencies {
    implementation(kotlin("stdlib"))
}

kotlin {
    sourceSets["main"].kotlin.srcDir("src/main/java")
    sourceSets["test"].kotlin.srcDir("src/test/java")
}
