# Ultima-Grid Administrative Command Deck
## "What is 'Terminal Mastery', Alex?"

### 0. What is the "Chain of Command"?
*Hierarchy for Grid administrative oversight.*
*   **Tier 0:** Master Administrator (Final Deletion Override)
*   **Tier 1:** Gemini Overseer (Neural Coordination)
*   **Tier 2:** Network Administrator (Infrastructure Oversight)
*   **Tier 2.5:** Judiciary Counsel (Legal & IP Oversight)
*   **Tier 3:** Grid Administrator (Resource Sign-off)
*   **Tier 4.5:** System Debugger (Neural Trace)
*   **Tier 4:** Neural Translator (Moderator Logic)
*   **Tier 5:** Vault Treasure (Financial Services)
*   **Tier 6:** Operative Program (User Control)

### 1. What is "Grid Deployment"?
*Commands for compiling the Web UI, IAR Firmware, and Android APK.*

```powershell
# Run the full build suite (Diagnostics -> Web UI -> IAR -> Android)
powershell -ExecutionPolicy Bypass -File .\scripts\build_suite.ps1

# Clean the build caches (The "Matrix Reset")
.\gradlew clean
```

### 2. I'll take "Hardware Handshakes" for $400
*ADB and connectivity logic. Stay on target, Red Leader.*

```powershell
# Auto-detect device IP and start the DMX Sync Engine
powershell -ExecutionPolicy Bypass -File .\scripts\run_sync_with_adb.ps1

# Filter logs for the Neural Translator and Vibe Checks
adb logcat -s "TRANSLATOR" -s "VIBE" -s "ALOUD"

# Verify hardware MIDI device connection status
adb shell dumpsys usb | grep "Midi"
```

### 3. I'll take "Neural API Logic" for $600
*Direct REST queries to the local 8080 hub. I find your lack of path disturbing.*

```powershell
# Query the full system telemetry (The "Total State")
curl http://localhost:8080/api/state

# Remote Trigger: Activate the Fog Machine (1500ms spark)
Invoke-RestMethod -Uri "http://localhost:8080/api/trigger?fx=fog"

# Remote Control: Set Sub-Bass to Overdrive (Level 1.0)
Invoke-RestMethod -Uri "http://localhost:8080/api/control?param=sub&value=1.0"
```

### 4. I'll take "Security Handshakes" for $800
*Verifying Layer S1 (AI) and S2 (Auth) credentials, GCP Secret Manager sync, and Vault caching.*

```powershell
# Sync secrets from Google Cloud Secret Manager to Windows Vault
powershell -ExecutionPolicy Bypass -File .\scripts\sync_gcp_secrets.ps1

# Provision credentials manually directly into the secure Windows Vault
powershell -ExecutionPolicy Bypass -File .\scripts\provision_credentials.ps1 -S1_Key "GEMINI_KEY" -S2_Key "OAUTH_CLIENT_ID"

# Wipes/deprovisions all stored UltimaGrid credentials from the Vault
powershell -ExecutionPolicy Bypass -File .\scripts\deprovision_credentials.ps1

# Run the system diagnostic and Gemini API handshake test
powershell -ExecutionPolicy Bypass -File .\foo.ps1

# Verify environmental variable burial
Get-ChildItem Env:UG_S1, Env:UG_S2
```

### 5. I'll take "Administrative Overrides" for $1000
*Mastery Level: Rogue. For when the door is closed and the Grid is dark.*

```powershell
# Force apply a specific preset via ID (Reference LOGIC_INDEX.md)
Invoke-RestMethod -Uri "http://localhost:8080/api/preset/apply?id=1"

# Secure override: Initiate a system repair via POST
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/admin/troubleshoot"
```

> **DAILY DOUBLE:** If the grid security protocol fails, remember: "These are not the MIDI controllers you're looking for."