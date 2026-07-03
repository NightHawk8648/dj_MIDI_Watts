package com.example.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread
import com.example.djmidiwatts.BuildConfig


/**
 * A lightweight embedded HTTP server to host the Web UI Commander and 
 * provide a bridge between the Web browser and the Android ViewModel.
 */
class WebUIHost(private val viewModel: CommanderViewModel, private val context: Context) {
    private var serverSocket: ServerSocket? = null
    private var authToken: String? = null
    private var isRunning = false
    private val MAX_CONCURRENT_CLIENTS = 20  // Limit to prevent resource exhaustion

    fun start(port: Int) {
        isRunning = true
        kotlin.concurrent.thread(start = true, name = "WebUIHostThread") {
            val bindAddress = "0.0.0.0" // Bind to all available network interfaces for network testing
            try {
                serverSocket = ServerSocket()
                serverSocket?.reuseAddress = true // Allow immediate rebinding after crashes
                val configPorts = BuildConfig.SERVER_FALLBACK_PORTS.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                val portsToTry = (listOf(port) + configPorts).distinct()
                var boundPort = -1
                for (p in portsToTry) {
                    try {
                        serverSocket?.bind(InetSocketAddress(InetAddress.getByName(bindAddress), p))
                        boundPort = p
                        break
                    } catch (e: java.net.BindException) {
                        viewModel.logMessage("[WARN] Port $p in use, trying next fallback...")
                        serverSocket = ServerSocket() // Recreate socket if bind failed
                        serverSocket?.reuseAddress = true
                    }
                }
                if (boundPort != -1) {
                    viewModel.logMessage("[WEB] Server active at http://${viewModel.localIpAddress}:$boundPort (bound to $bindAddress)")
                } else {
                    throw java.net.BindException("All fallback ports (${BuildConfig.SERVER_FALLBACK_PORTS}) are currently in use.")
                }
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    if ((viewModel.activeConnections.toString().toIntOrNull() ?: 0) >= MAX_CONCURRENT_CLIENTS) {
                        socket.close()
                        continue
                    }
                    // Handle each client in a new thread to prevent blocking the server's accept loop
                    kotlin.concurrent.thread { handleClient(socket) }
                }
            } catch (e: Exception) {
                if (isRunning) viewModel.logMessage("[ERROR] WebHost: ${e.localizedMessage}")
            }
        }
    }

    fun stop() {
        isRunning = false
        serverSocket?.close()
        serverSocket = null
    }

    private fun parseParams(query: String): Map<String, String> {
        return query.split("&").filter { it.contains("=") }.associate { 
            val split = it.split("=")
            split[0] to split.getOrElse(1) { "" }
        }
    }

    private fun handleClient(socket: Socket) {
        viewModel.activeConnections++ // Increment count for this active client handler
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            val method = parts.getOrNull(0) ?: "GET"
            val fullPath = parts.getOrNull(1) ?: "/"

            // Consume headers and identify Content-Length for POST payloads
            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
                if (line!!.startsWith("Authorization:", ignoreCase = true)) {
                    authToken = line!!.substringAfter("Bearer ").trim()
                }
            }

            // Extract path and query string for parameter parsing
            val path = fullPath.substringBefore("?")
            var rawQuery = fullPath.substringAfter("?", "")

            // Capture POST body as query parameters for unified processing
            if (method == "POST" && contentLength > 0) {
                val body = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val n = reader.read(body, totalRead, contentLength - totalRead)
                    if (n == -1) break
                    totalRead += n
                }
                val bodyStr = String(body, 0, totalRead)
                rawQuery = if (rawQuery.isEmpty()) bodyStr else "$rawQuery&$bodyStr"
            }
            
            // What is 'URL Decoding'? Sanitizing incoming signals for the Translator.
            val query = URLDecoder.decode(rawQuery, StandardCharsets.UTF_8.name())

            val output = DataOutputStream(socket.getOutputStream())

            // Handle CORS preflight (Universal OS/Browser support)
            if (method == "OPTIONS") {
                sendResponse(output, "text/plain", "".toByteArray())
                return
            }

            // Validate Wireless Injection Credentials
            val isAuthorized = if (BuildConfig.UG_S2.isEmpty() || BuildConfig.UG_S2 == "YOUR_CLIENT_ID.apps.googleusercontent.com") {
                false // Block injection if the system hasn't been provisioned
            } else {
                authToken == BuildConfig.UG_S2
            }

            if (!isAuthorized && (path.contains("/api/control") || path.contains("/api/trigger"))) {
                viewModel.logMessage("[WARN] Injection Blocked: Missing or Invalid OAuth Token.")
                sendResponse(output, "application/json", "{\"error\":\"Unauthorized Injection attempt\"}".toByteArray(), "401 Unauthorized")
                return
            }

            if (isAuthorized && path.contains("/api/control")) {
                viewModel.logMessage("[SECURE] Certificate verified for wireless command injection.")
            }

            if (path == "/api/control") {
                // Parse fader adjustment parameters (e.g., ?param=low&value=0.8)
                val params = parseParams(query)
                val param = params["param"]
                val valueStr = params["value"] ?: ""

                when (param) {
                    "answer" -> {
                        viewModel.submitDailyDoubleAnswer(valueStr)
                        sendResponse(output, "application/json", "{\"status\":\"answered\", \"val\":\"$valueStr\"}".toByteArray())
                    }
                    "confirm" -> {
                        viewModel.confirmSevereAction()
                        sendResponse(output, "application/json", "{\"status\":\"confirmed\"}".toByteArray())
                    }
                    else -> {
                        val value = valueStr.toFloatOrNull() ?: 0.5f
                        viewModel.updateFader(param ?: "", value)
                        sendResponse(output, "application/json", "{\"status\":\"ok\", \"param\":\"$param\", \"val\":$value}".toByteArray())
                    }
                }
            } else if (path == "/api/trigger") {
                // Parse trigger parameters (e.g., ?fx=laser or ?fx=fog)
                val params = parseParams(query)
                val fx = params["fx"]

                when (fx) {
                    "laser" -> viewModel.triggerLaserQuick()
                    "fog" -> viewModel.triggerFogQuick(1500)
                }
                sendResponse(output, "application/json", "{\"status\":\"triggered\", \"fx\":\"$fx\"}".toByteArray())
            } else if (path.startsWith("/api/state")) {
                val responseJson = when (path) {
                    "/api/state/telemetry" -> JSONObject().apply {
                        put("memory_usage", viewModel.memoryUsagePercent)
                        put("storage_efficiency", viewModel.storageEfficiency)
                        put("network_latency_ms", viewModel.networkPingMs)
                        put("active_sockets", viewModel.activeConnections)
                        put("cloud_status", JSONObject().apply {
                            put("enabled", viewModel.isPersonalCloudEnabled)
                            put("sync_state", viewModel.cloudSyncStatus)
                        })
                        put("max_effort", viewModel.maximumEffortActive)
                    }
                    "/api/state/vault" -> JSONObject().apply {
                        put("vault_balance", viewModel.vaultBalance)
                        put("history", JSONArray().apply {
                            viewModel.transactionHistory.forEach { tx ->
                                put(JSONObject().apply {
                                    put("title", tx.title)
                                    put("amount", tx.amount)
                                    put("date", tx.date)
                                })
                            }
                        })
                    }
                    "/api/state/debug" -> JSONObject().apply {
                        put("trace_active", viewModel.isDebugTraceActive)
                        put("last_report", viewModel.lastDebugReport)
                        put("unresolved_references", viewModel.unresolvedReferenceCount)
                        put("tier", "Debugger (4.5)")
                    }
                    "/api/state/legal" -> JSONObject().apply {
                        put("eula_signed", viewModel.isNeuralEulaSigned)
                        put("compliance", viewModel.legalComplianceStatus)
                        put("tier", "Judiciary (2.5)")
                    }
                    "/api/state/security" -> JSONObject().apply {
                        put("license", viewModel.gridLicenseStatus)
                        put("status", viewModel.securityIntegrityLevel)
                        put("last_check", viewModel.lastSecurityCheckDate)
                    }
                    "/api/state/hardware" -> JSONObject().apply {
                        put("sync_status", viewModel.isMidiHardwareConnected)
                        put("bpm", viewModel.bpmVal)
                        put("visual_mode", viewModel.visualMode)
                        put("strobe_active", viewModel.isStrobeActive)
                        put("laser_active", viewModel.isLaserActive)
                        put("faders", JSONObject().apply {
                            put("low", viewModel.faderLow.toDouble())
                            put("mid", viewModel.faderMid.toDouble())
                            put("high", viewModel.faderHigh.toDouble())
                            put("vocal", viewModel.faderVocal.toDouble())
                            put("sub", viewModel.faderSub.toDouble())
                        })
                    }
                    else -> {
                        // Legacy / Comprehensive State (Aggregated Neural Link)
                        JSONObject().apply {
                            put("bpm", viewModel.bpmVal)
                            put("visual_mode", viewModel.visualMode)
                            put("strobe_active", viewModel.isStrobeActive)
                            put("fog_active", viewModel.isFogActive)
                            put("laser_active", viewModel.isLaserActive)
                            put("sync_status", viewModel.isMidiHardwareConnected)
                            put("faders", JSONObject().apply {
                                put("low", viewModel.faderLow.toDouble())
                                put("mid", viewModel.faderMid.toDouble())
                                put("high", viewModel.faderHigh.toDouble())
                                put("vocal", viewModel.faderVocal.toDouble())
                                put("sub", viewModel.faderSub.toDouble())
                            })
                            put("telemetry", JSONObject().apply {
                                put("memory_usage", viewModel.memoryUsagePercent)
                                put("network_latency_ms", viewModel.networkPingMs)
                            })
                        }
                    }
                }
                sendResponse(output, "application/json", responseJson.toString().toByteArray())
            } else if (path == "/api/admin/scan") {
                viewModel.runSystemWasteScan()
                sendResponse(output, "application/json", "{\"status\":\"scanning\"}".toByteArray())
            } else if (path == "/api/admin/dispose") {
                viewModel.performWasteDisposal()
                sendResponse(output, "application/json", "{\"status\":\"pending_confirmation\"}".toByteArray())
            } else if (path == "/api/admin/integrity") {
                viewModel.runSecurityIntegrityCheck()
                sendResponse(output, "application/json", "{\"status\":\"audit_initiated\"}".toByteArray())
            } else if (path == "/api/admin/sovereignty") {
                viewModel.runSecurityIntegrityCheck()
                sendResponse(output, "application/json", "{\"status\":\"sovereignty_audit_started\"}".toByteArray())
            } else if (path == "/api/admin/legal") {
                viewModel.runLegalComplianceAudit()
                sendResponse(output, "application/json", "{\"status\":\"judiciary_audit_initiated\"}".toByteArray())
            } else if (path == "/api/admin/debug") {
                viewModel.runGlobalNeuralTrace()
                sendResponse(output, "application/json", "{\"status\":\"trace_initiated\"}".toByteArray())
            } else if (path == "/api/admin/cloud_test") {
                viewModel.testCloudConnectivity()
                sendResponse(output, "application/json", "{\"status\":\"cloud_handshake_initiated\"}".toByteArray())
            } else if (path == "/api/admin/troubleshoot") {
                viewModel.runSystemTroubleshoot()
                sendResponse(output, "application/json", "{\"status\":\"repair_sequence_started\"}".toByteArray())
            } else if (path == "/api/admin/swede") {
                viewModel.swedeTheGrid()
                sendResponse(output, "application/json", "{\"status\":\"grid_sweded\"}".toByteArray())
            } else if (path == "/api/admin/wellness") {
                viewModel.triggerWellnessCheck()
                sendResponse(output, "application/json", "{\"status\":\"wellness_triggered\"}".toByteArray())
            } else if (path.startsWith("/api/v1/presets")) {
                // Simple API bridge simulation
                val json = "{\"status\":\"active\", \"count\": ${viewModel.allPresets.value.size}}"
                sendResponse(output, "application/json", json.toByteArray())
            } else {
                // Serve static assets from the bundled web-commander folder
                val assetPath = if (path == "/" || path.isEmpty()) "index.html" else path.removePrefix("/")
                try {
                    val bytes = context.assets.open("web-commander/$assetPath").readBytes()
                    val mime = when {
                        assetPath.endsWith(".html") -> "text/html"
                        assetPath.endsWith(".js") -> "application/javascript"
                        assetPath.endsWith(".css") -> "text/css"
                        else -> "application/octet-stream"
                    }
                    sendResponse(output, mime, bytes)
                } catch (e: FileNotFoundException) {
                    sendResponse(output, "text/plain", "HTTP Error 404: Resource path not found.".toByteArray(), "404 Not Found")
                }
            }
        } catch (e: Exception) {
            // Log exceptions during client handling for debugging network issues
            e.printStackTrace()
            // Silently drop anomalies to maintain Grid stability
        } finally {
            viewModel.activeConnections--
            try { socket.close() } catch (e: IOException) { }
        }
    }

    private fun sendResponse(out: DataOutputStream, mime: String, data: ByteArray, status: String = "200 OK") {
        try {
            out.write("HTTP/1.1 $status\r\n".toByteArray(StandardCharsets.UTF_8))
            out.write("Content-Type: $mime\r\n".toByteArray(StandardCharsets.UTF_8))
            out.write("Connection: close\r\n".toByteArray(StandardCharsets.UTF_8))
            out.write("Server: Ultima-Grid-Host/1.0\r\n".toByteArray(StandardCharsets.UTF_8))
            out.write("Access-Control-Allow-Origin: *\r\n".toByteArray(StandardCharsets.UTF_8))
            out.write("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n".toByteArray(StandardCharsets.UTF_8))
            out.write("Content-Length: ${data.size}\r\n\r\n".toByteArray(StandardCharsets.UTF_8))
            out.write(data)
            out.flush()
        } catch (e: IOException) { }
    }
}