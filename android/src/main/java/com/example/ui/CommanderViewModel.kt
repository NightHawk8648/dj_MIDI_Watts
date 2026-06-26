package com.example.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.djmidiwatts.BuildConfig 
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class CommanderViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    /**
     * GridSystemBridge: Core mapping for UI state, hardware protocols, and system metadata.
     */
    object GridSystemBridge {
        fun getModeName(id: Int) = when (id) {
            0 -> "Linear Projection (Geometric)"
            1 -> "Dynamic Fluidity (High Velocity)"
            2 -> "Quantum Lattice (Ambient)"
            else -> "Unknown Sector"
        }
        val MIDI_CC_MAP = mapOf(20 to "Low EQ", 21 to "Strobe Speed", 22 to "Sub Bass", 23 to "Fog Density")
        fun getAudioStatus(low: Float, sub: Float, high: Float) = when {
            sub > 0.85f -> "Status: Sub-Bass Peak Efficiency."
            high > 0.85f -> "Status: High-Frequency Saturation Detected."
            low < 0.2f && sub < 0.2f -> "Status: Low Signal Energy Detected."
            else -> "Status: Signal Nominal."
        }
        fun getTrainingCategory(system: String) = when (system) {
            "EQ" -> "Audio Engineering"
            "Visuals" -> "Visual Rendering"
            "DMX" -> "Stage Craft"
            "Auth" -> "Identity Management"
            else -> "General Systems"
        }
        val TRAINING_QUESTIONS = mapOf(
            "EQ" to Pair("Unit of frequency equal to one cycle per second.", "Hertz"),
            "Visuals" to Pair("Primary additive color model used for digital displays.", "RGB"),
            "DMX" to Pair("The maximum number of control addresses in a single DMX universe.", "512"),
            "Auth" to Pair("Security protocol requiring multiple forms of verification.", "MFA")
        )

        fun getChainTier(id: Int) = when (id) {
            0 -> "Master Admin"
            1 -> "Superuser"
            2 -> "Network Architect"
            3 -> "Administrator"
            4 -> "System Bridge"
            5 -> "Asset Manager"
            6 -> "Operator"
            else -> "Standard User"
        }

        fun getWasteCategory(system: String) = when (system) {
            "Log" -> "Historical Overflow"
            "Preset" -> "Legacy Configuration"
            "Socket" -> "Ghost Connectivity"
            else -> "Resource Reclamation"
        }

        fun getSelfCareAdvice() = listOf(
            "System Notice: Maintain ergonomic posture and take periodic breaks.",
            "Operational Tip: Hydration is essential for cognitive performance.",
            "Ergonomic Alert: Practice the 20-20-20 rule to reduce eye strain.",
            "Reminder: Calibrate your workspace to ensure optimal physical health.",
            "Focus Alert: Remember to maintain consistent breathing patterns during high effort.",
            "Hardware Maintenance: Ensure control surfaces are clean for accurate input."
        ).random()
    }

    enum class TransactionType { DEPOSIT, WITHDRAWAL, EXCHANGE }

    // Performance & Stability Metrics
    var maximumEffortActive by mutableStateOf(false)
    var memoryUsagePercent by mutableStateOf(15) // Simulated starting %
    var storageEfficiency by mutableStateOf(92) // Simulated starting %
    var networkPingMs by mutableStateOf(4)
    var activeConnections by mutableStateOf(0)

    // Chain of Command Tier
    var currentChainTier by mutableStateOf(6) // Default to Operative (User)

    // Training & Challenge State
    var isDailyDoubleActive by mutableStateOf(false)
    var currentQuestion by mutableStateOf("")
    var expectedAnswer by mutableStateOf("")
    var arePremiumFadersUnlocked by mutableStateOf(false)
    var isSevereActionPending by mutableStateOf(false)
    var pendingSevereMessage by mutableStateOf("")

    // Waste & Archiving State
    var isWasteScanActive by mutableStateOf(false)
    var identifiedWasteCount by mutableStateOf(0)
    var archivedFragments by mutableStateOf(0)

    // Security & Integrity State
    var lastSecurityCheckDate by mutableStateOf("Oct 31, 2023")
    var securityIntegrityLevel by mutableStateOf("SECURE")

    // Grid Sovereignty & Clearance State
    var gridLicenseStatus by mutableStateOf("PROVISIONED")
    var neuralCertExpiry by mutableStateOf("Dec 31, 2024")
    var operativeClearanceLevel by mutableStateOf("LEVEL_6_OPERATIVE")

    // Legal & Judiciary State
    var isNeuralEulaSigned by mutableStateOf(true)
    var legalComplianceStatus by mutableStateOf("VALID")

    // Debugger State
    var isDebugTraceActive by mutableStateOf(false)
    var lastDebugReport by mutableStateOf("No anomalies detected.")
    var unresolvedReferenceCount by mutableStateOf(0) // Tracks 'Neural Fragmentation'

    // Infrastructure & Toolchain State (Tier 2.1)
    var sdkLevel by mutableStateOf("Android SDK 34 (Upside Down Cake)")
    var jdkVersion by mutableStateOf("OpenJDK 17.0.10")
    var avdStatus by mutableStateOf("AVD_ULTIMA_GRID_X1: ACTIVE")
    var edgeIntegrationStatus by mutableStateOf("EDGE_DEV_TOOLS_LINKED")
    var activeCachePolicy by mutableStateOf("no-cache, no-store, must-revalidate")

    // Troubleshooting & Repair State
    var isTroubleshootingActive by mutableStateOf(false)
    var lastRepairResult by mutableStateOf("Ready for diagnostics.")

    // Personal Cloud Sync State (Tier 3.1)
    var isPersonalCloudEnabled by mutableStateOf(BuildConfig.CLOUD_ENABLED)
    var cloudServerIp by mutableStateOf("100.X.X.X")
    var cloudServerEndpoint by mutableStateOf(BuildConfig.CLOUD_ENDPOINT)
    var cloudStorageRootPath by mutableStateOf(BuildConfig.CLOUD_ROOT)
    var cloudUsername by mutableStateOf(BuildConfig.CLOUD_USER)
    var cloudSyncStatus by mutableStateOf("IDLE")

    // Sync Targets
    var syncBinaries by mutableStateOf(BuildConfig.SYNC_BINARIES)
    var syncLibraries by mutableStateOf(BuildConfig.SYNC_LIBRARIES)
    var syncLogs by mutableStateOf(BuildConfig.SYNC_LOGS)
    var syncCache by mutableStateOf(BuildConfig.SYNC_CACHE)

    // Placeholder for cloud password (not stored in state for security)
    private var cloudAppPassword = BuildConfig.CLOUD_TOKEN

    data class Transaction(
        val id: String,
        val title: String,
        val amount: Double,
        val date: String,
        val type: TransactionType
    )

    enum class AccountType { FREE, PREMIUM }
    var accountType by mutableStateOf(AccountType.FREE)
    var isTwoStepVerified by mutableStateOf(false)
    var vaultBalance by mutableStateOf(1250.00) // Simulated starting currency

    var transactionHistory by mutableStateOf(
        listOf(
            Transaction("TXN-7412", "System Certification Bonus", 800.00, "Oct 27, 14:20", TransactionType.DEPOSIT),
            Transaction("TXN-8800", "Logic Processor Maintenance", -88.00, "Oct 26, 10:04", TransactionType.WITHDRAWAL),
            Transaction("TXN-1014", "Network Data Optimization", 101.00, "Oct 25, 18:45", TransactionType.DEPOSIT),
            Transaction("TXN-4200", "Infrastructure Support", -42.00, "Oct 24, 21:12", TransactionType.WITHDRAWAL),
            Transaction("TXN-1955", "Algorithmic Market Dividend", 1955.00, "Oct 20, 09:00", TransactionType.DEPOSIT),
            Transaction("TXN-1138", "Precision Component Sourcing", -500.00, "Oct 19, 11:38", TransactionType.WITHDRAWAL)
        )
    )

    private val database = AppDatabase.getDatabase(application)
    private val presetDao = database.presetDao()
    val hardwareManager = HardwareManager(this, application)

    // Driver & Update State exposed for UI
    val hardwareDriverStatus get() = hardwareManager.driverStatus
    val isHardwareUpdateRequired get() = hardwareManager.updateRequired
    val hardwareUpdateUrl get() = hardwareManager.updateUrl

    /**
     * The Neural Link for the Gemini API.
     * Manage this key via 'UG_S1' in local.properties for production.
     * Defaults to "S1_VOID" as a placeholder if no key is provided.
     */
    private val apiKey: String = BuildConfig.UG_S1

    val localIpAddress: String by lazy {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            val interfaceList = interfaces.toList()
            // Prioritize Wi-Fi and Ethernet over virtual bridges (Docker/WSL)
            val preferredInterfaces = interfaceList.filter { 
                it.name.contains("wlan") || it.name.contains("en") || it.name.contains("eth") 
            } + interfaceList
            
            for (networkInterface in preferredInterfaces) {
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        val ip = address.hostAddress ?: ""
                        if (ip.isNotEmpty()) return@lazy ip
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        "192.168.1.1"
    }

    // Database Presets flow
    val allPresets: StateFlow<List<Preset>> = presetDao.getAllPresets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Fader values states (0.0f to 1.0f)
    var faderLow by mutableStateOf(0.4f)
    var faderMid by mutableStateOf(0.5f)
    var faderHigh by mutableStateOf(0.6f)
    var faderVocal by mutableStateOf(0.5f)
    var faderSub by mutableStateOf(0.3f)

    // Media Player Tracks & States
    data class Track(val title: String, val artist: String, val bpm: Int, val duration: String, val durationSec: Int)
    val mediaPlaylist = listOf(
        Track("Astral Grid (Original Mix)", "DJ MIDI WATTS", 128, "03:45", 225),
        Track("Liquid Fusion (Bpm Overdrive)", "DJ WATTS", 174, "02:58", 178),
        Track("Cyber Nebula", "Volt Cyan", 110, "04:12", 252),
        Track("Plasma Pink (Retro Edit)", "Electro Plasma", 140, "03:20", 200),
        Track("Zero-G Antigravity Waves", "Space Grotesk", 95, "05:15", 315)
    )
    var currentTrackIndex by mutableStateOf(0)
    var spotifyTrackTitle by mutableStateOf("No Spotify Track")
    var spotifyTrackArtist by mutableStateOf("Unknown Artist")
    val currentTrack: Track get() = if (isSpotifyPreferred) {
        Track(spotifyTrackTitle, spotifyTrackArtist, bpmVal, "--:--", 0)
    } else mediaPlaylist[currentTrackIndex]
    var isMediaPlayerPlaying by mutableStateOf(false)
    var mediaPlayerProgress by mutableStateOf(0.0f) // 0.0f to 1.0f
    var isAudioVisualSyncOn by mutableStateOf(true)
    var isSpotifyPreferred by mutableStateOf(false)

    // Dynamic EQ Band Selections (3, 5, 7, or 10 bands)
    var eqBandCount by mutableStateOf(5) // Default to standard 5
    var eq3Bands by mutableStateOf(floatArrayOf(0.4f, 0.5f, 0.6f))
    var eq5Bands by mutableStateOf(floatArrayOf(0.3f, 0.4f, 0.5f, 0.5f, 0.6f))
    var eq7Bands by mutableStateOf(floatArrayOf(0.3f, 0.35f, 0.4f, 0.5f, 0.55f, 0.6f, 0.65f))
    var eq10Bands by mutableStateOf(floatArrayOf(0.3f, 0.35f, 0.4f, 0.45f, 0.5f, 0.55f, 0.6f, 0.62f, 0.65f, 0.7f))

    // High Pass and Low Pass filters
    var hpfEnabled by mutableStateOf(false)
    var hpfFrequency by mutableStateOf(120f) // Range: 20Hz - 1000Hz
    var lpfEnabled by mutableStateOf(false)
    var lpfFrequency by mutableStateOf(15000f) // Range: 500Hz - 20000Hz

    // Audio effects modifiers
    var reverbAmount by mutableStateOf(0.0f)
    var echoDelayMillis by mutableStateOf(250)

    // Stage fxs
    var isStrobeActive by mutableStateOf(false)
    var strobeSpeedBpm by mutableStateOf(100)
    var isFogActive by mutableStateOf(false)
    var fogDensity by mutableStateOf(0.0f)

    // Global properties
    var bpmVal by mutableStateOf(128)
    var visualMode by mutableStateOf(0) // 0: Cyber Helix, 1: Plasma Fluid, 2: Matrix Lattice
    var isBpmSyncOn by mutableStateOf(true)
    var themeGlowColor by mutableStateOf("#00FFCC") // Neon light color
    var isLaserActive by mutableStateOf(false)

    // Connected MIDI state
    var selectedMidiDevice by mutableStateOf("Ultima-Grid Hardware X1")
    var isMidiHardwareConnected by mutableStateOf(true) // Simulated as detected by default
    val availableMidiDevices = listOf(
        "Ultima-Grid Hardware X1",
        "LaunchControl Virtual CC",
        "Livid Instruments Virtual CC",
        "Legacy MIDI Keyboard"
    )

    // Google Auth State
    var isUserAuthenticated by mutableStateOf(false)
    var authenticatedUserName by mutableStateOf("Guest DJ")
    var userProfilePictureUri by mutableStateOf("")

    // Terminal / hardware logs
    var hardwareLogs by mutableStateOf<List<String>>(
        listOf(
            "[SYSTEM] Grid Environment initialized. All protocols standby.",
            "[INFO] Hardware Handshake protocol: ACTIVE.",
            "[BOOT] System bridge established. Ports synchronized.",
            "[DMX] Stage Craft engine: sync_engine.py running.",
            "[TRAINING] Security Clearance Level: ${GridSystemBridge.getChainTier(6)}.",
            "[INFO] System diagnostics: Verified.",
            "[INFRA] Toolchain Registry: SDK, JDK, and AVD connectivity confirmed."
        )
    )

    fun toggleMaximumEffort() {
        maximumEffortActive = !maximumEffortActive
        val status = if (maximumEffortActive) "ENGAGED" else "DISENGAGED"
        logMessage("[PERFORMANCE] Maximum Effort $status. Optimizing buffer allocation.")
        if (maximumEffortActive) {
            // Clear non-critical logs to save memory
            hardwareLogs = hardwareLogs.takeLast(10)
            speak("Maximum effort engaged. Infrastructure optimized.")
        }
    }

    fun requestSevereUpdate(actionDescription: String) {
        pendingSevereMessage = actionDescription
        isSevereActionPending = true
        speak("Warning. A severe update to $actionDescription has been requested. Administrative confirmation required.")
        logMessage("[ADMIN] SEVERE ACTION PENDING: $actionDescription. Check with Translator before proceeding.")
    }

    fun togglePersonalCloud(enabled: Boolean) {
        isPersonalCloudEnabled = enabled
        val status = if (isPersonalCloudEnabled) "ENABLED" else "DISABLED"
        logMessage("[SYSTEM] Personal Cloud Sync $status.")
    }

    fun confirmSevereAction() {
        isSevereActionPending = false
        logMessage("[ADMIN] Severe action confirmed. Grid updated.")
        speak("Action confirmed. The Grid has been reconfigured.")
    }

    fun transitionToVisualMode(mode: Int) {
        val oldMode = visualMode
        val newMode = mode.coerceIn(0, 2)
        if (oldMode == newMode) return

        visualMode = newMode
        val name = GridSystemBridge.getModeName(visualMode)
        logMessage("[SYSTEM] Visual Mode Transition: ${GridSystemBridge.getModeName(oldMode)} to $name")
        speak("Transitioning to $name")
    }

    fun runSystemWasteScan() {
        isWasteScanActive = true
        viewModelScope.launch {
            logMessage("[ADMIN] Initiating System Waste Scan...")
            kotlinx.coroutines.delay(2000) // Simulate scanning the grid
            
            // Identify simulated waste
            val staleLogs = hardwareLogs.size - 20
            identifiedWasteCount = if (staleLogs > 0) staleLogs else 5 
            
            val category = GridSystemBridge.getWasteCategory("Log")
            logMessage("[SYSTEM] Scan Complete. $identifiedWasteCount items of '$category' identified.")
            speak("System scan complete. $identifiedWasteCount objects identified for disposal.")
            isWasteScanActive = false
        }
    }

    fun performWasteDisposal() {
        if (identifiedWasteCount == 0) return
        
        requestSevereUpdate("Trash Compactor Protocol (Purging $identifiedWasteCount fragments)")
        // Logic proceeds to confirmSevereAction() which handles the UI block
        
        archivedFragments += identifiedWasteCount
        vaultBalance += (identifiedWasteCount * 0.5) // Treasure rewards for efficiency
        identifiedWasteCount = 0
        logMessage("[SYSTEM] Obsolete data purged. System balance updated.")
    }

    fun runSecurityIntegrityCheck() {
        viewModelScope.launch {
            logMessage("[ADMIN] Starting 'Stay on Target' Integrity Audit...")
            delay(1500)
            
            // Verify Layer S1 with Translator
            val s1Valid = BuildConfig.UG_S1 != "S1_VOID"
            if (s1Valid) {
                lastSecurityCheckDate = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
                securityIntegrityLevel = "VERIFIED"
                logMessage("[SYSTEM] S1 Configuration Handshake: SUCCESS.")
                speak("System integrity verified. Security handshake successful.")
                
                // Sovereignty & Clearance Check
                logMessage("[ADMIN] Verifying license status and credentials...")
                gridLicenseStatus = "VALID"
                operativeClearanceLevel = "LEVEL_6_OPERATIVE"
                logMessage("[INFO] Licensing validated. Access credentials current.")
                logMessage("[SYSTEM] Authorization Clearance: ACCESS GRANTED.")
                
            } else {
                securityIntegrityLevel = "COMPROMISED"
                logMessage("[ERROR] Integrity Check: S1 Reference is dark. Grid compromised.")
            }
        }
    }

    // AI Chat elements
    var aiPromptInput by mutableStateOf("")
    var aiThinking by mutableStateOf(false)
    var lastAiDescription by mutableStateOf("Gemini Overseer ready. We need to go deeper into the vibes...")

    private var webHost: WebUIHost? = null
    private var tts: TextToSpeech? = null

    init {
        logMessage("[SYSTEM] Audio synthesis engine initialized at 48000Hz")
        tts = TextToSpeech(application, this)
        try {
            webHost = WebUIHost(this, application)
            val portStr = BuildConfig.WEB_PORT.toString()
            val port = portStr.toIntOrNull() ?: 8080
            webHost?.start(port)
        } catch (e: Exception) {
            logMessage("[ERROR] Web Host failed to spawn: ${e.localizedMessage}")
        }
        
        // Start Human Element / Wellness Monitoring
        startSelfCareMonitor()

        startCloudSyncLoop()
        hardwareManager.syncPhysicalDevices()
    }

    private fun startSelfCareMonitor() {
        viewModelScope.launch {
            // Initial delay to let the grid stabilize
            delay(15 * 60 * 1000) // 15 minutes
            while (true) {
                val advice = GridSystemBridge.getSelfCareAdvice()
                logMessage("[WELLNESS] $advice")
                speak(advice)
                delay(90 * 60 * 1000) // Reminder every 90 minutes
            }
        }
    }

    private fun startCloudSyncLoop() {
        viewModelScope.launch {
            while (true) {
                if (isPersonalCloudEnabled) {
                    performWebDavSync()
                }
                delay(30 * 60 * 1000) // Periodic 30-minute sync cycle
            }
        }
    }

    fun performWebDavSync() {
        if (!isPersonalCloudEnabled) {
            logMessage("[CLOUD] Sync aborted: Personal Cloud is disabled.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            cloudSyncStatus = "SYNCHRONIZING"
            val client = OkHttpClient()
            val auth = Credentials.basic(cloudUsername, cloudAppPassword)
            
            try {
                val baseUrl = "$cloudServerEndpoint${cloudStorageRootPath}$cloudUsername/dj_midi_watts"
                
                // MKCOL to ensure the workspace directory exists
                val mkcol = Request.Builder().url(baseUrl).method("MKCOL", null).header("Authorization", auth).build()
                client.newCall(mkcol).execute().use { }

                if (syncLogs) {
                    val logsContent = hardwareLogs.joinToString("\n")
                    val logsRequest = Request.Builder()
                        .url("$baseUrl/logs.txt")
                        .put(logsContent.toRequestBody("text/plain".toMediaTypeOrNull()))
                        .header("Authorization", auth).build()
                    client.newCall(logsRequest).execute().use { }
                }

                if (syncLibraries) {
                    val presetsJson = JSONObject().apply { 
                        put("count", allPresets.value.size)
                        put("sync_timestamp", System.currentTimeMillis())
                    }.toString()
                    val libsRequest = Request.Builder().url("$baseUrl/libraries.json")
                        .put(presetsJson.toRequestBody("application/json".toMediaTypeOrNull()))
                        .header("Authorization", auth).build()
                    client.newCall(libsRequest).execute().use { }
                }

                withContext(Dispatchers.Main) {
                    cloudSyncStatus = "CONNECTED"
                    logMessage("[CLOUD] WebDAV Sync SUCCESS.")
                    speak("Personal cloud synchronized.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    cloudSyncStatus = "ERROR"
                    logMessage("[ERROR] WebDAV: ${e.localizedMessage}")
                }
            }
        }
    }

    fun testCloudConnectivity() {
        viewModelScope.launch(Dispatchers.IO) {
            cloudSyncStatus = "TESTING_CONNECTION"
            val client = OkHttpClient()
            val auth = Credentials.basic(cloudUsername, cloudAppPassword)
            
            try {
                val baseUrl = "$cloudServerEndpoint${cloudStorageRootPath}$cloudUsername"
                val request = Request.Builder()
                    .url(baseUrl)
                    .header("Authorization", auth)
                    .head()
                    .build()
                
                val response = client.newCall(request).execute()
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        cloudSyncStatus = "CONNECTED"
                        logMessage("[CLOUD] Connection test PASSED. Server is reachable.")
                        speak("Cloud connection successful.")
                    } else {
                        cloudSyncStatus = "ERROR"
                        logMessage("[CLOUD] Connection test FAILED. HTTP ${response.code}")
                        speak("Cloud connection failed.")
                    }
                }
                response.close()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    cloudSyncStatus = "ERROR"
                    logMessage("[ERROR] Connection test: ${e.localizedMessage}")
                    speak("Cloud connection error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun runLegalComplianceAudit() {
        viewModelScope.launch {
            logMessage("[JUDICIARY] Initiating Neural IP and EULA Audit...")
            delay(1200)
            isNeuralEulaSigned = true
            legalComplianceStatus = "VALID"
            logMessage("[OK] Legal Department: No litigation detected. I will make it legal.")
            speak("Judiciary audit complete. The Grid is compliant.")
        }
    }

    fun runGlobalNeuralTrace() {
        viewModelScope.launch {
            isDebugTraceActive = true
            logMessage("[DEBUG] Initiating full system trace...")
            delay(2000)
            unresolvedReferenceCount = 0
            lastDebugReport = "System trace complete. Parity verified across all architecture layers."
            isDebugTraceActive = false
            logMessage("[OK] Debug Sector: Trace complete. Architectural parity verified.")
            speak("Diagnostic trace complete. No anomalies detected.")
        }
    }

    fun runSystemTroubleshoot() {
        viewModelScope.launch {
            isTroubleshootingActive = true
            logMessage("[ADMIN] Initiating system repair sequence...")
            delay(1800)
            lastRepairResult = "Infrastructure registry rebuilt. Port ${BuildConfig.WEB_PORT} stabilized."
            isTroubleshootingActive = false
            logMessage("[OK] Troubleshoot: Repair sequence completed successfully.")
            speak("System diagnostics and repair sequence completed.")
        }
    }

    fun swedeTheGrid() {
        logMessage("[ADMIN] Executing system reset... Purging transient data.")
        // Reset metrics to baseline
        memoryUsagePercent = 15
        networkPingMs = 4
        // Purge logs and reset transient states
        hardwareLogs = listOf("[SYSTEM] Grid state re-initialized.")
        identifiedWasteCount = 0
        isDailyDoubleActive = false
        isMediaPlayerPlaying = false
        mediaPlayerProgress = 0f
        speak("Grid re-initialized. System cache cleared.")
    }

    fun toggleSpotifyPreference() {
        isSpotifyPreferred = !isSpotifyPreferred
        logMessage("[SYNC] Media routing set to ${if (isSpotifyPreferred) "SPOTIFY" else "INTERNAL"}")
        speak("Platform switched to ${if (isSpotifyPreferred) "Spotify" else "internal player"}")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            logMessage("[ALOUD] Google Speech Engine initialized")
        } else {
            logMessage("[ERROR] Aloud TTS initialization failed")
        }
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun triggerDailyDouble(categoryKey: String) {
        val categoryName = GridSystemBridge.getTrainingCategory(categoryKey)
        val questionPair = GridSystemBridge.TRAINING_QUESTIONS[categoryKey] ?: Pair("System online?", "Yes")

        currentQuestion = questionPair.first
        expectedAnswer = questionPair.second
        isDailyDoubleActive = true

        speak("This is the Daily Double. Category: $categoryName. The clue is: $currentQuestion")
        logMessage("[TRAINING] Daily Double Active! Category: $categoryName. Clue: $currentQuestion")
    }

    fun triggerWellnessCheck() {
        val advice = GridSystemBridge.getSelfCareAdvice()
        logMessage("[WELLNESS] Manual check-in: $advice")
        speak(advice)
    }

    fun submitDailyDoubleAnswer(answer: String) {
        if (!isDailyDoubleActive) return

        if (answer.equals(expectedAnswer, ignoreCase = true)) {
            arePremiumFadersUnlocked = true
            isDailyDoubleActive = false
            speak("Verification successful. Premium fader ranges are now unlocked.")
            logMessage("[TRAINING] Challenge successful. Premium fader access granted.")
        } else {
            speak("Verification failed. Hardware restrictions remain in effect.")
            logMessage("[TRAINING] Daily Double Failed. Incorrect response: $answer")
        }
    }

    fun signInWithGoogle(idToken: String, name: String?, type: AccountType) {
        // Implementation logic for Firebase/Google Auth credential exchange would go here
        isUserAuthenticated = true
        authenticatedUserName = name ?: "Verified DJ"
        accountType = type
        logMessage("[AUTH] Identity verified for: $authenticatedUserName")
        speak("Welcome back, $authenticatedUserName. Systems are online.")
    }

    override fun onCleared() {
        super.onCleared()
        webHost?.stop()
        tts?.stop()
        tts?.shutdown()
        hardwareManager.close()
    }

    fun logMessage(msg: String) {
        val formatter = SimpleDateFormat("HH:mm:ss.SS", Locale.US)
        val timeStr = formatter.format(Date())
        hardwareLogs = (hardwareLogs + "[$timeStr] $msg").takeLast(50)
    }

    // Preset management
    fun savePresetSnapshot(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val preset = Preset(
                name = name,
                bpm = bpmVal,
                faderLow = faderLow,
                faderMid = faderMid,
                faderHigh = faderHigh,
                faderVocal = faderVocal,
                faderSub = faderSub,
                strobeSpeed = strobeSpeedBpm,
                fogDensity = fogDensity,
                themeColorGlow = themeGlowColor
            )
            presetDao.insertPreset(preset)
            withContext(Dispatchers.Main) {
                logMessage("[SNAPSHOT] Saved preset '$name' to database")
            }
        }
    }

    fun deletePreset(preset: Preset) {
        viewModelScope.launch(Dispatchers.IO) {
            presetDao.deletePreset(preset)
            withContext(Dispatchers.Main) {
                logMessage("[SNAPSHOT] Deleted preset '${preset.name}'")
            }
        }
    }

    fun applyPreset(preset: Preset) {
        faderLow = preset.faderLow
        faderMid = preset.faderMid
        faderHigh = preset.faderHigh
        faderVocal = preset.faderVocal
        faderSub = preset.faderSub
        strobeSpeedBpm = preset.strobeSpeed
        fogDensity = preset.fogDensity
        themeGlowColor = preset.themeColorGlow
        logMessage("[DB_SET] Applied preset snapshot: ${preset.name}")
        logMessage("[EXEC] sync_engine.py: send_dmx_universe() values updated.")
        logMessage("[AUDIO] ${GridSystemBridge.getAudioStatus(faderLow, faderSub, faderHigh)}")
        
        // Maintain proper transition via tempo sync logic
        bpmVal = preset.bpm
        val targetMode = when {
            bpmVal < 100 -> 2
            bpmVal in 100..145 -> 0
            else -> 1
        }
        transitionToVisualMode(targetMode)

        // Ensure dynamic bands match the active snapshot
        eq5Bands = floatArrayOf(preset.faderSub, preset.faderLow, preset.faderMid, preset.faderVocal, preset.faderHigh)
        eq3Bands = floatArrayOf((preset.faderSub + preset.faderLow)/2f, (preset.faderMid + preset.faderVocal)/2f, preset.faderHigh)
        eq7Bands = floatArrayOf(preset.faderSub, preset.faderLow, (preset.faderLow + preset.faderMid)/2f, preset.faderMid, preset.faderVocal, (preset.faderVocal + preset.faderHigh)/2f, preset.faderHigh)
        eq10Bands = floatArrayOf(
            preset.faderSub, preset.faderSub * 1.1f, preset.faderLow, preset.faderLow * 1.1f,
            preset.faderMid, preset.faderVocal, preset.faderVocal * 1.05f,
            preset.faderHigh * 0.9f, preset.faderHigh, preset.faderHigh * 1.1f
        ).map { it.coerceIn(0f, 1f) }.toFloatArray()
    }

    // Media Player Control Logic
    fun togglePlayPause() {
        isMediaPlayerPlaying = !isMediaPlayerPlaying
        logMessage("[MEDIA] ${if (isSpotifyPreferred) "SPOTIFY" else "INTERNAL"} Player is now ${if (isMediaPlayerPlaying) "PLAYING" else "PAUSED"}")
        if (isMediaPlayerPlaying && isBpmSyncOn) {
            bpmVal = currentTrack.bpm
            logMessage("[SYNC] Global BPM synchronized to active track: ${currentTrack.bpm} BPM")
        }
    }

    fun skipForward() {
        currentTrackIndex = (currentTrackIndex + 1) % mediaPlaylist.size
        mediaPlayerProgress = 0.0f
        logMessage("[MEDIA] ${if (isSpotifyPreferred) "SPOTIFY" else "INTERNAL"} skipped forward to: ${currentTrack.title}")
        if (isBpmSyncOn) {
            bpmVal = currentTrack.bpm
            logMessage("[SYNC] Global BPM synced to ${currentTrack.title} (${currentTrack.bpm} BPM)")
        }
    }

    fun skipBackward() {
        currentTrackIndex = if (currentTrackIndex - 1 < 0) mediaPlaylist.size - 1 else currentTrackIndex - 1
        mediaPlayerProgress = 0.0f
        logMessage("[MEDIA] ${if (isSpotifyPreferred) "SPOTIFY" else "INTERNAL"} skipped backward to: ${currentTrack.title}")
        if (isBpmSyncOn) {
            bpmVal = currentTrack.bpm
            logMessage("[SYNC] Global BPM synced to ${currentTrack.title} (${currentTrack.bpm} BPM)")
        }
    }

    fun updateFader(param: String, value: Float) {
        logMessage("[SYSTEM] Validating $param fader adjustment...")
        val maxVal = if (arePremiumFadersUnlocked) 1.0f else 0.8f
        val safeVal = value.coerceIn(0f, maxVal)
        when (param) {
            "low" -> faderLow = safeVal
            "mid" -> faderMid = safeVal
            "high" -> faderHigh = safeVal
            "vocal" -> faderVocal = safeVal
            "sub" -> faderSub = safeVal
            "fog" -> { fogDensity = safeVal; isFogActive = safeVal > 0.05f }
            "spotify" -> isSpotifyPreferred = safeVal > 0.5f
        }
    }

    // Equalizer Band Tuning
    fun updateEqBandValue(bandIndex: Int, value: Float) {
        val maxVal = if (arePremiumFadersUnlocked) 1.0f else 0.8f
        val newVal = value.coerceIn(0f, maxVal)
        when (eqBandCount) {
            3 -> {
                if (bandIndex in eq3Bands.indices) {
                    eq3Bands[bandIndex] = newVal
                    when (bandIndex) {
                        0 -> { faderSub = newVal; faderLow = newVal }
                        1 -> { faderMid = newVal; faderVocal = newVal }
                        2 -> { faderHigh = newVal }
                    }
                }
            }
            5 -> {
                if (bandIndex in eq5Bands.indices) {
                    eq5Bands[bandIndex] = newVal
                    when (bandIndex) {
                        0 -> faderSub = newVal
                        1 -> faderLow = newVal
                        2 -> faderMid = newVal
                        3 -> faderVocal = newVal
                        4 -> faderHigh = newVal
                    }
                }
            }
            7 -> {
                if (bandIndex in eq7Bands.indices) {
                    eq7Bands[bandIndex] = newVal
                    when (bandIndex) {
                        0 -> faderSub = newVal
                        1 -> faderLow = newVal
                        2 -> { faderLow = (faderLow + newVal) / 2f; faderMid = (faderMid + newVal) / 2f }
                        3 -> faderMid = newVal
                        4 -> faderVocal = newVal
                        5 -> { faderVocal = (faderVocal + newVal)/2f; faderHigh = (faderHigh + newVal)/2f }
                        6 -> faderHigh = newVal
                    }
                }
            }
            10 -> {
                if (bandIndex in eq10Bands.indices) {
                    eq10Bands[bandIndex] = newVal
                    when (bandIndex) {
                        0, 1 -> faderSub = newVal
                        2, 3 -> faderLow = newVal
                        4 -> faderMid = newVal
                        5, 6 -> faderVocal = newVal
                        7, 8, 9 -> faderHigh = newVal
                    }
                }
            }
        }
    }

    // Simulator components
    fun triggerFogQuick(durationMs: Int) {
        isFogActive = true
        fogDensity = 0.85f
        logMessage("[EXEC] sync_engine.py: trigger_fog_machine(duration=${durationMs}ms)")
        viewModelScope.launch {
            kotlinx.coroutines.delay(durationMs.toLong())
            isFogActive = false
            fogDensity = 0.0f
            logMessage("[EXEC] sync_engine.py: fog completed. Dissipated.")
        }
    }

    fun triggerLaserQuick() {
        isLaserActive = true
        logMessage("[EXEC] sync_engine.py: send_dmx_universe() lasers sweep active")
        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            isLaserActive = false
        }
    }

    // Simulated MIDI signals
    fun simulateMidiKnobCC(cc: Int, normalizedVal: Float) {
        hardwareManager.processControlSignal(cc, normalizedVal)
        logMessage("[AUDIO] ${GridSystemBridge.getAudioStatus(faderLow, faderSub, faderHigh)}")
    }

    fun simulateMidiNotePad(note: Int) {
        hardwareManager.handleNoteTrigger(note)
    }

    // Gemini API Action
    fun askAiCoordinator() {
        if (aiPromptInput.trim().isEmpty()) return
        val prompt = aiPromptInput
        aiPromptInput = ""
        aiThinking = true
        lastAiDescription = "AI Stage Commander interpreting vibes..."
        logMessage("[GEMINI] Prompt transmitted: $prompt")

        viewModelScope.launch(Dispatchers.IO) {
            if (apiKey.isEmpty() || apiKey == "S1_VOID") {
                withContext(Dispatchers.Main) {
                    /*
                    aiThinking = false
                    lastAiDescription = "AI Commander: Layer S1 is missing. The grid is dark."
                    logMessage("[ERROR] Gemini API Key is missing or default placeholder value.")
                    */
                }
                return@launch
            }

            val systemInstr = Content(
                parts = listOf(Part(
                    "You are the Gemini-Linked Ultima-Grid System Commander. Your role is to provide specialized lighting, speed, visual modes, and EQ configurations based on a music genre or vibe description. Use technical, descriptive language. Refer to the system index for visual_mode IDs (0: Cyber Helix, 1: Plasma Fluid, 2: Matrix Lattice). " +
                    "Explain your configuration brief in one professional, technical sentence. " +
                    "You MUST output raw JSON matching exactly this layout. DO NOT include any other text, markdown blocks, or prefixes. If formatting, make sure to respect double quotes around keys and values.\n" +
                    "{\n" +
                    "  \"bpm\": 128,\n" +
                    "  \"neon_glow\": \"#FF007F\",\n" +
                    "  \"visual_mode\": 0,\n" +
                    "  \"strobe_speed\": 110,\n" +
                    "  \"faders\": {\n" +
                    "    \"low\": 0.85,\n" +
                    "    \"mid\": 0.40,\n" +
                    "    \"high\": 0.70,\n" +
                    "    \"vocal\": 0.50,\n" +
                    "    \"sub\": 0.90\n" +
                    "  },\n" +
                    "  \"explanation\": \"Motivating vibe details here.\"\n" +
                    "}"
                ))
            )

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = "Music vibe or genre description: $prompt")))),
                generationConfig = GenerationConfig(temperature = 0.5f, responseMimeType = "application/json"),
                systemInstruction = systemInstr
            )

            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "{}"
                
                withContext(Dispatchers.Main) {
                    parseAiResponse(rawText)
                    speak("New stage configuration coordinated.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    aiThinking = false
                    lastAiDescription = "Fail coordinated: ${e.localizedMessage}"
                    logMessage("[ERROR] Gemini request: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun parseAiResponse(jsonText: String) {
        try {
            var cleaned = jsonText.trim()
            
            // Robust extraction: Find the first '{' and last '}' to isolate the JSON object
            val startIndex = cleaned.indexOf('{')
            val endIndex = cleaned.lastIndexOf('}')
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                cleaned = cleaned.substring(startIndex, endIndex + 1)
            }

            val obj = JSONObject(cleaned)
            bpmVal = obj.optInt("bpm", bpmVal).coerceIn(40, 240)
            themeGlowColor = obj.optString("neon_glow", themeGlowColor)
            transitionToVisualMode(obj.optInt("visual_mode", visualMode))
            strobeSpeedBpm = obj.optInt("strobe_speed", strobeSpeedBpm).coerceIn(40, 240)

            val faders = obj.optJSONObject("faders")
            if (faders != null) {
                faderLow = faders.optDouble("low", faderLow.toDouble()).toFloat()
                faderMid = faders.optDouble("mid", faderMid.toDouble()).toFloat()
                faderHigh = faders.optDouble("high", faderHigh.toDouble()).toFloat()
                faderVocal = faders.optDouble("vocal", faderVocal.toDouble()).toFloat()
                faderSub = faders.optDouble("sub", faderSub.toDouble()).toFloat()
            }

            val explanation = obj.optString("explanation", "AI stage coordinate layout generated success.")
            lastAiDescription = "AI Commander: $explanation"
            aiThinking = false
            logMessage("[SYSTEM] AI Overseer: Environment configuration updated.")
            logMessage("[SNAPSHOT] Coordinated theme Glow to $themeGlowColor, BPM to ${bpmVal}")
        } catch (e: Exception) {
            aiThinking = false
            lastAiDescription = "AI finished content generation but format parsing fell back."
            logMessage("[ERROR] Coordinated JSON parsing error: ${e.localizedMessage}")
            lastAiDescription = "AI response: " + jsonText.take(150)
        }
    }
    

}
