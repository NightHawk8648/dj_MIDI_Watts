import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.io.ByteArrayOutputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

// Inherit JVM toolchain from root
kotlin {
  jvmToolchain(17)
}

val slf4jLogger = LoggerFactory.getLogger("global-secrets-logger")

fun logAudit(message: String) {
    slf4jLogger.info(message)
    try {
        val auditFile = project.layout.buildDirectory.file("outputs/secrets_audit.log").get().asFile
        if (!auditFile.parentFile.exists()) auditFile.parentFile.mkdirs()
        val timestamp = LocalDateTime.now().toString()
        auditFile.appendText("[$timestamp] $message\n")
    } catch (e: Exception) {
        slf4jLogger.warn("Failed to write to secrets_audit.log: ${e.message}")
    }
}

fun commandExists(command: String): Boolean {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val checkArgs = if (isWindows) listOf("cmd", "/c", "where", command) else listOf("sh", "-c", "command -v $command")
    return try {
        ProcessBuilder(checkArgs).redirectErrorStream(true).start().waitFor() == 0
    } catch (e: Exception) {
        false
    }
}

fun findExecutable(vararg candidates: String): String? {
    return candidates.firstOrNull { commandExists(it) }
}

fun getPythonCmd(): String {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val possiblePaths = if (isWindows) {
        listOf(".venv/Scripts/python.exe", ".venv/bin/python.exe", ".venv/bin/python")
    } else {
        listOf(".venv/bin/python", ".venv/Scripts/python.exe", ".venv/bin/python.exe")
    }
    for (path in possiblePaths) {
        val file = project.rootDir.resolve(path)
        if (file.exists()) {
            return file.absolutePath
        }
    }
    return if (isWindows) "python" else "python3"
}

fun escapeBuildConfigString(value: Any?): String {
    val strValue = value?.toString()?.trim() ?: ""
    return if (strValue.startsWith("\"") && strValue.endsWith("\"")) {
        strValue
    } else {
        "\"" + strValue.replace("\"", "\\\"") + "\""
    }
}

fun loadEnvFile(fileName: String): Map<String, String> {
    val envMap = mutableMapOf<String, String>()
    val envFile = project.rootDir.resolve(fileName)
    if (envFile.exists()) {
        envFile.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    var value = parts[1].trim()
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length - 1)
                    } else if (value.startsWith("'") && value.endsWith("'")) {
                        value = value.substring(1, value.length - 1)
                    }
                    envMap[key] = value
                }
            }
        }
    }
    return envMap
}

fun loadWifConfig(fileName: String): String {
    val wifFile = project.rootDir.resolve(fileName)
    if (wifFile.exists()) {
        val text = wifFile.readText()
        val match = Regex("\"attributeCondition\"\\s*:\\s*\"([^\"]+)\"").find(text)
        return match?.groupValues?.get(1) ?: ""
    }
    return ""
}

fun getSecretFromVault(resource: String, username: String): String {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    if (!isWindows) return ""
    return try {
        val output = ByteArrayOutputStream()
        val command = "[Windows.Security.Credentials.PasswordVault, Windows.Security.Credentials, ContentType=WindowsRuntime] | Out-Null; \$v = New-Object Windows.Security.Credentials.PasswordVault; Write-Host -NoNewline \$v.Retrieve('$resource', '$username').Password"
        project.exec {
            commandLine("powershell", "-Command", command)
            standardOutput = output
            isIgnoreExitValue = true
        }
        output.toString().trim()
    } catch (e: Exception) {
        ""
    }
}

android {
  namespace = "com.example.djmidiwatts"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.djmidiwatts.vbtqkx"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Load WIF Config & Cloud Env configs
    val cloudEnv = loadEnvFile("cloud_env.env")
    val googleClientId = loadWifConfig("wif-config.json")

    // Fetch S1/S2 with Windows Vault fallback
    var s1 = project.findProperty("UG_S1")?.toString() ?: ""
    if (s1.isEmpty() || s1 == "S1_VOID") {
        s1 = getSecretFromVault("UltimaGrid", "UG_S1")
    }
    var s2 = project.findProperty("UG_S2")?.toString() ?: ""
    if (s2.isEmpty() || s2 == "S2_VOID") {
        s2 = getSecretFromVault("UltimaGrid", "UG_S2")
    }

    // Bridge secrets from the plugin (project properties) into string resources.
    resValue("string", "ug_s1", s1)
    resValue("string", "ug_s2", s2)

    // Explicitly map secrets to manifest placeholders. 
    manifestPlaceholders["MAPS_API_KEY"] = s1

    // Configure the Web Bridge port for simultaneous VS Code / Android Studio dev
    val webPort = project.findProperty("WEB_PORT")?.toString() ?: "8080"
    buildConfigField("int", "WEB_PORT", webPort)
    
    // Cloud configuration
    val cloudEnabled = project.findProperty("CLOUD_ENABLED")?.toString() ?: "false"
    buildConfigField("Boolean", "CLOUD_ENABLED", cloudEnabled)
    buildConfigField("String", "CLOUD_ENDPOINT", escapeBuildConfigString(project.findProperty("CLOUD_ENDPOINT")))
    buildConfigField("String", "CLOUD_ROOT", escapeBuildConfigString(project.findProperty("CLOUD_ROOT")))
    buildConfigField("String", "CLOUD_USER", escapeBuildConfigString(project.findProperty("CLOUD_USER")))
    buildConfigField("String", "CLOUD_TOKEN", escapeBuildConfigString(project.findProperty("CLOUD_TOKEN")))
    buildConfigField("Boolean", "SYNC_BINARIES", project.findProperty("SYNC_BINARIES")?.toString() ?: "false")
    buildConfigField("Boolean", "SYNC_LIBRARIES", project.findProperty("SYNC_LIBRARIES")?.toString() ?: "false")
    buildConfigField("Boolean", "SYNC_LOGS", project.findProperty("SYNC_LOGS")?.toString() ?: "false")
    buildConfigField("Boolean", "SYNC_CACHE", project.findProperty("SYNC_CACHE")?.toString() ?: "false")
    buildConfigField("String", "UG_S1", escapeBuildConfigString(s1))
    buildConfigField("String", "UG_S2", escapeBuildConfigString(s2))
    
    // Shared Cloud Config Fields
    buildConfigField("String", "GCP_PROJECT_ID", escapeBuildConfigString(cloudEnv["GCP_PROJECT_ID"] ?: "dj-midi-watts"))
    buildConfigField("String", "GCP_REGION", escapeBuildConfigString(cloudEnv["GCP_REGION"] ?: "us-central1"))
    buildConfigField("String", "GCP_CLOUD_RUN_URL", escapeBuildConfigString(cloudEnv["GCP_CLOUD_RUN_URL"] ?: ""))
    buildConfigField("String", "ENVIRONMENT", escapeBuildConfigString(cloudEnv["ENVIRONMENT"] ?: "production"))
    buildConfigField("String", "GOOGLE_CLIENT_ID", escapeBuildConfigString(googleClientId))
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  
  buildFeatures {
    compose = true
    buildConfig = true
  }
  kotlinOptions {
    jvmTarget = "17"
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Copy APKs after build
tasks.register<Copy>("copyDebugApk") {
  from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
  into(rootDir.resolve("bin"))
  rename { "app-debug.apk" }
}

tasks.register<Copy>("copyReleaseApk") {
  from(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
  into(rootDir.resolve("bin"))
  rename { "app-release.apk" }
}

gradle.projectsEvaluated {
  tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy("copyDebugApk", "lockAuditLog")
  }
  tasks.matching { it.name == "assembleRelease" }.configureEach {
    finalizedBy("copyReleaseApk", "lockAuditLog")
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"

  // Add keys here that you want the plugin to ignore.
  // These will not be generated in BuildConfig or Manifest placeholders.
  ignoreList.add("UG_S3_KEYSTORE_PASS")
  ignoreList.add("sdk.dir") // Good practice to ignore standard Android properties
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(project(":flutter"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation("androidx.compose.animation:animation")
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
  
  // Consider moving these to libs.versions.toml for full consistency
  implementation("com.google.android.gms:play-services-auth:21.2.0")
  implementation("com.google.firebase:firebase-auth:23.1.0")
  
  implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
tasks.register("printSecrets") {
    val s1Debug = project.findProperty("UG_S1_DEBUG")
    val s1Release = project.findProperty("UG_S1_RELEASE")
    val ugS1 = System.getenv("UG_S1")
    val ugS2 = System.getenv("UG_S2")
    val gradleCertPath = System.getenv("GRADLE_CERT_PATH")

    doLast {
        logAudit("🔑 GEMINI_API_KEY = $ugS1")
        logAudit("🔑 GOOGLE_OAUTH_CLIENT_ID = $ugS2")
        logAudit("📜 CLIENT_CERT_PATH = $gradleCertPath")

        // Verify if the Secrets Plugin is pulling from the .env file into Project properties
        if (s1Debug != null || s1Release != null) {
            logAudit("\n✅ Secrets Plugin Verification: SUCCESS")
            logAudit("   -> Debug Key: ${if (s1Debug != null) "DETECTED" else "MISSING"}")
            logAudit("   -> Release Key: ${if (s1Release != null) "DETECTED" else "MISSING"}")
        } else {
            logAudit("\n⚠️ Secrets Plugin Verification: FAIL")
            logAudit("   -> No properties found. Ensure your '.env' file exists in the project root.")
        }
    }
}

// --- Web UI Bridge Tasks ---

val fixMissingWebUi by tasks.creating(Exec::class) {
    group = "web-ui"
    description = "Runs diagnostic fix if web-ui folder is missing."
    workingDir = project.rootDir
    onlyIf {
        val webUiMissing = !file("${project.rootDir}/web-ui").exists()
        webUiMissing
    }
    commandLine("powershell", "-ExecutionPolicy", "Bypass", "-File", "${project.rootDir}/foo.ps1", "-Fix")
}

val verifyNodeVersion by tasks.creating {
    group = "web-ui"
    description = "Verifies that the installed Node.js version meets the minimum requirement."
    dependsOn(fixMissingWebUi)

    onlyIf {
        val hasNode = commandExists("node")
        if (!hasNode) {
            slf4jLogger.info("⚠️ Node.js is not available. Skipping web-ui verification.")
        }
        hasNode
    }

    doLast {
        val minNodeVersion = "18.0.0"
        val output = ByteArrayOutputStream()
        exec {
            commandLine("node", "-v")
            standardOutput = output
        }
        val currentVersion = output.toString().trim().removePrefix("v")
        
        val currParts = currentVersion.split(".").map { part: String -> part.toIntOrNull() ?: 0 }
        val minParts = minNodeVersion.split(".").map { part: String -> part.toIntOrNull() ?: 0 }
        
        var isCompatible = true
        for (i in 0 until maxOf(currParts.size, minParts.size)) {
            val c = currParts.getOrElse(i) { 0 }
            val m = minParts.getOrElse(i) { 0 }
            if (c > m) break
            if (c < m) {
                isCompatible = false
                break
            }
        }

        if (!isCompatible) {
            throw GradleException("Node.js version mismatch! Found v$currentVersion, but v$minNodeVersion or higher is required.")
        }
        slf4jLogger.info("[OK] Node.js version verified: v$currentVersion")
    }
}

val buildWebUi by tasks.registering {
    dependsOn(verifyNodeVersion)
    group = "web-ui"
    description = "Builds the web UI assets for the Android application."

    onlyIf {
        val pkgJson = file("${project.rootDir}/web-ui/package.json")
        if (!pkgJson.exists()) return@onlyIf false
        val pkgText = pkgJson.readText()
        val placeholderBuild = "echo 'Placeholder build: Web UI assets ready.'"
        pkgText.contains("build") && !pkgText.contains(placeholderBuild)
    }

    doLast {
        val pkgJson = file("${project.rootDir}/web-ui/package.json")
        if (!pkgJson.exists()) {
            slf4jLogger.info("⚠️ web-ui package.json missing; skipping web-ui build.")
            return@doLast
        }
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val npmExecutable = if (isWindows) findExecutable("npm.cmd", "npm") else findExecutable("npm", "npm.cmd")
        if (npmExecutable == null) {
            slf4jLogger.info("⚠️ npm is not available. Skipping web-ui build.")
            return@doLast
        }

        val webUiDir = file("${project.rootDir}/web-ui")
        val nodeModulesDir = file("${project.rootDir}/web-ui/node_modules")
        if (!nodeModulesDir.exists()) {
            slf4jLogger.info("Running npm install in web-ui directory...")
            exec {
                workingDir = webUiDir
                commandLine(npmExecutable, "install")
            }
        }

        exec {
            workingDir = webUiDir
            commandLine(npmExecutable, "run", "build")
        }
    }
}

val copyWebUiAssets by tasks.creating(Copy::class) {
    dependsOn(buildWebUi)
    
    onlyIf {
        file("${project.rootDir}/web-ui/dist").exists()
    }

    from("${project.rootDir}/web-ui/dist")
    into("$projectDir/src/main/assets/web-commander")
}

// Ensure assets are compiled and bridged before every build
tasks.preBuild {
    dependsOn(copyWebUiAssets)
}

tasks.register("generateDotEnv") {
    group = "setup"
    description = "Generates a .env file with default values if it's missing."

    val dotEnvFile = project.rootDir.resolve(".env")

    doLast {
        if (!dotEnvFile.exists()) {
            logAudit("⚠️ .env file not found. Generating with default values.")
            dotEnvFile.writeText("""
                WEB_PORT=8080
                UG_S1=S1_VOID
                UG_S2=S2_VOID
                # Add other environment variables here as needed
            """.trimIndent())
            logAudit("✅ .env file generated at ${dotEnvFile.absolutePath}")
        } else {
            logAudit("ℹ️ .env file already exists. Skipping generation.")
        }
    }
}

// --- Ultimate Assembly & Orchestration ---

tasks.register<Exec>("runGridAuditor") {
    group = "verification"
    description = "Runs the Tier 4.5 Python Auditor to check for Neural Fragmentation."
    workingDir = rootDir
    val pythonCmd = getPythonCmd()
    commandLine(pythonCmd, "scripts/grid_auditor.py")
    
    standardOutput = System.out
    errorOutput = System.err
}

tasks.register("ultimateGridAssembly") {
    group = "build"
    description = "The Master Assembly: Synchronizes all teams for deployment readiness."
    
    // Chain all necessary components
    finalizedBy("lockAuditLog", "runFinalDiagnostics")
    dependsOn("runGridAuditor")
    dependsOn("printSecrets")
    dependsOn("copyWebUiAssets")
    dependsOn("assembleRelease")
    dependsOn("copyReleaseApk")
    
    // Ensure tests pass before claiming "Ready for Publication"
    dependsOn("testReleaseUnitTest")

    doLast {
        slf4jLogger.info("\n" + "=".repeat(60))
        slf4jLogger.info("🚀 ULTIMA-GRID ASSEMBLY COMPLETE")
        slf4jLogger.info("📦 Android APK: bin/app-release.apk")
        slf4jLogger.info("🌐 Web UI: Embedded in Assets")
        slf4jLogger.info("⚖️ Deployment Status: SIGNED, TESTED, AND READY FOR PUBLICATION")
        slf4jLogger.info("=".repeat(60))
    }
}

// --- Example Custom Python Task ---

val installPythonDependencies by tasks.registering(Exec::class) {
    group = "custom"
    description = "Installs Python dependencies from requirements.txt via pip."
    
    val requirementsPath = project.rootDir.resolve("scripts/requirements.txt").absolutePath
    val markerFile = layout.buildDirectory.file("pip_install.marker")

    // Incremental Build: Gradle will skip this task if requirements.txt hasn't changed.
    inputs.file(requirementsPath)
    outputs.file(markerFile)

    val pythonCmd = getPythonCmd()
    
    workingDir = rootDir
    commandLine(pythonCmd, "-m", "pip", "install", "-r", requirementsPath)

    doLast {
        markerFile.get().asFile.writeText("Success: ${System.currentTimeMillis()}")
    }
}

tasks.register("runPythonSync") {
    dependsOn(installPythonDependencies)
    group = "custom"
    description = "Runs the python sync engine in the background with auto-detected IP."
    
    val pythonCmd = getPythonCmd()

    doLast {
        val manualArgs = (project.findProperty("pyArgs") as? String)?.split(" ")
        val finalArgs = mutableListOf(pythonCmd, "scripts/sync_engine.py")
        val webPort = project.findProperty("WEB_PORT")?.toString() ?: "8080"

        if (manualArgs != null) {
            finalArgs.addAll(manualArgs)
        } else {
            // Attempt to auto-detect the Android IP via ADB
            val output = ByteArrayOutputStream()
            exec {
                commandLine("adb", "shell", "ip", "route")
                standardOutput = output
                isIgnoreExitValue = true
            }
            
            val match = Regex("src\\s+([0-9.]+)").find(output.toString())
            val deviceIp = match?.groupValues?.get(1)

            if (deviceIp != null) {
                slf4jLogger.info("📡 Auto-detected Android Host IP: $deviceIp")
                finalArgs.addAll(listOf("--ip", deviceIp))
            } else {
                slf4jLogger.info("⚠️ No device detected via ADB. Falling back to --offline.")
                finalArgs.add("--offline")
            }
        }

        // Pass the web port to the Python script
        finalArgs.addAll(listOf("--port", webPort))

        // Background execution logic using ProcessBuilder
        val processBuilder = ProcessBuilder(finalArgs)
        processBuilder.directory(rootDir)
        
        // Redirect output to a log file to prevent blocking and allow auditing
        val logFile = file("${project.layout.buildDirectory.get()}/outputs/python_sync.log")
        logFile.parentFile.mkdirs()
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
        processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(logFile))
        
        val process = processBuilder.start()
        
        // Save the PID to a file so it can be stopped later
        file("${project.layout.buildDirectory.get()}/python_sync.pid").writeText(process.pid().toString())
        
        slf4jLogger.info("🚀 Sync Engine (PID ${process.pid()}) started in background.")
        slf4jLogger.info("📝 Logs: ${logFile.absolutePath}")
    }
}

tasks.register("stopPythonSync") {
    group = "custom"
    description = "Stops the background Python sync engine using the saved PID."
    
    doLast {
        val pidFile = file("${project.layout.buildDirectory.get()}/python_sync.pid")
        if (pidFile.exists()) {
            val pid = pidFile.readText().trim()
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            
            exec {
                if (isWindows) {
                    // Remove /F to send a close message instead of a forced termination
                    commandLine("taskkill", "/T", "/PID", pid)
                } else {
                    commandLine("kill", "-15", pid) // -15 is SIGTERM (graceful)
                }
                isIgnoreExitValue = true
            }
            pidFile.delete()
            slf4jLogger.info("🛑 Sync Engine (PID $pid) stopped.")
        } else {
            slf4jLogger.info("ℹ️ No Sync Engine PID file found. It might not be running.")
        }
    }
}

tasks.preBuild {
    dependsOn("generateDotEnv")
}

tasks.register<Exec>("runFinalDiagnostics") {
    group = "verification"
    description = "Runs the system diagnostic utility (foo.ps1) after assembly."
    workingDir = rootDir
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val shell = if (isWindows) "powershell" else "pwsh"
    val webPort = project.findProperty("WEB_PORT")?.toString() ?: "8080"
    commandLine(shell, "-ExecutionPolicy", "Bypass", "-File", file("${rootDir}/foo.ps1"), "-WebPort", webPort)
    standardOutput = System.out
    errorOutput = System.err
}

tasks.register("lockAuditLog") {
    group = "verification"
    description = "Makes the audit log read-only to prevent tampering after the build finishes."
    doLast {
        val auditFile = project.layout.buildDirectory.file("outputs/secrets_audit.log").get().asFile
        if (auditFile.exists()) {
            if (auditFile.setReadOnly()) {
                slf4jLogger.info("🔒 Security: secrets_audit.log has been locked (read-only).")
            }
        }
    }
}
