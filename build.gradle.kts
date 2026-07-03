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

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Zip

// Shared root-level tasks
tasks.register<Zip>("zipExtension") {
    group = "build"
    description = "Zips the extension for browser installation"
    from("dj-midi-watts-extension")
    include("**/*")
    exclude("**/*.pem", "**/*.crx", "**/*.ps1", "STORE_JUSTIFICATIONS.md")
    archiveFileName.set("dj-midi-watts-extension.zip")
    destinationDirectory.set(layout.buildDirectory)
}

tasks.register<Exec>("buildAndroidApk") {
    group = "build"
    description = "Builds the Android APK via PowerShell Orchestrator"
    commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File", "scripts/build_mobile_apps.ps1", "-Android")
}

tasks.register<Exec>("buildIosApp") {
    group = "build"
    description = "Builds the iOS App via PowerShell Orchestrator"
    commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File", "scripts/build_mobile_apps.ps1", "-Ios")
}

tasks.register<Exec>("buildDesktopMsix") {
    group = "build"
    description = "Builds the MSIX Desktop Package"
    commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File", "scripts/build_desktop_msix.ps1")
}


dependencies {
    implementation(kotlin("stdlib"))
}

kotlin {
    sourceSets["main"].kotlin.srcDir("src/main/java")
    sourceSets["test"].kotlin.srcDir("src/test/java")
}
