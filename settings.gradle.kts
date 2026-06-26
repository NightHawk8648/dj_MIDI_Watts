pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

// plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "DJ MIDI WATTS"

include(":android")

val flutterProjectRoot = File(settingsDir, "flutter_ui")
val includeFlutterScript = File(flutterProjectRoot, ".android/include_flutter.groovy")

if (includeFlutterScript.exists()) {
    apply(from = includeFlutterScript)
}
