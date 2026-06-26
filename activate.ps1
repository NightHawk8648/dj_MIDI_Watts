# DJ MIDI WATTS - Ultima-Grid System Activation Script

param (
    [switch]$Refresh,
    [switch]$SkipSync,
    [int]$WebPort = 8080, # Added WebPort parameter
    [switch]$DeepTest
)

Write-Host "--- [SYSTEM] Initializing Ultima-Grid: Listing Chain of Command. ---" -ForegroundColor Cyan

# 1. Path Neutrality Check (Tier-2 Infrastructure)
$ScriptDir = $PSScriptRoot
if (-not $ScriptDir) {
    $ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
}

if ($DeepTest) {
    Write-Host "`n--- [TESTING] Executing Deep-Dive Diagnostics ---" -ForegroundColor Blue
    # File System Integrity Check
    $criticalFiles = @("main.cpp", "gradlew.bat", "local.properties", ".env") # Added .env to critical files
    foreach ($file in $criticalFiles) {
        if (Test-Path (Join-Path $ScriptDir $file)) {
            Write-Host "[OK] Integrity: $file verified." -ForegroundColor Green
        } else {
            Write-Host "[FAIL] Missing critical file: $file" -ForegroundColor Red
        }
    }
    # Networking/API Check (Updated to use WebPort)
    $apiCheck = curl.exe -s -o /dev/null -w "%{http_code}" "http://localhost:$($WebPort)/api/state"
    if ($apiCheck -eq "200") {
        Write-Host "[OK] Networking: Port $($WebPort) API Handshake successful." -ForegroundColor Green
    } else {
        Write-Host "[WARN] Networking: API Hub ($($WebPort)) unreachable. Proceeding in Offline Mode." -ForegroundColor Yellow
    }
}

$syncEnginePath = Join-Path $ScriptDir "scripts/sync_engine.py"
$coreLogicPath = Join-Path $ScriptDir "android/src/main/java/com/example/ui/CommanderViewModel.kt"


if (-not (Test-Path $syncEnginePath)) {
    Write-Host "[ERROR] Sync Engine not found at $syncEnginePath. Grid collapse imminent!" -ForegroundColor Red
    exit
}

# 2. Environment Verification (S1/S2 Handshake)
Write-Host "`n--- [SECURITY] Verifying Neural Credentials ---" -ForegroundColor Yellow
$s1 = $env:UG_S1
$s2 = $env:UG_S2

if (-not $s1 -or $s1 -eq "S1_VOID") {
    try {
        $vault = New-Object Windows.Security.Credentials.PasswordVault
        if (-not $s1) { $s1 = $vault.Retrieve("UltimaGrid", "UG_S1").Password }
        if (-not $s2) { $s2 = $vault.Retrieve("UltimaGrid", "UG_S2").Password }
        if ($s1) { Write-Host "[OK] Secure Vault handshake successful." -ForegroundColor Green }
    } catch {
        # Vault missing entry, fallback to local.properties
    }
}

if (-not $s1 -or $s1 -eq "S1_VOID") {
    $localProps = Join-Path $ScriptDir "local.properties"
    if (Test-Path $localProps) {
        $props = Get-Content $localProps
        foreach ($line in $props) {
            if ($line -match "UG_S1=(.*)") { $s1 = $Matches[1].Trim().Trim('"') }
            if ($line -match "UG_S2=(.*)") { $s2 = $Matches[1].Trim().Trim('"') }
        }
    }
}

if (-not $s1 -or $s1 -eq "S1_VOID") {
    $userEnv = Join-Path $ScriptDir "user.env"
    if (Test-Path $userEnv) {
        $lines = Get-Content $userEnv
        foreach ($line in $lines) {
            if ($line -match "^UG_S1=(.*)") { $s1 = $Matches[1].Trim().Trim('`" ') }
            if ($line -match "^UG_S2=(.*)") { $s2 = $Matches[1].Trim().Trim('`" ') }
        }
    }
}

if ($s1 -and $s1 -notmatch "VOID") {
    Write-Host "[OK] Layer S1/S2 Handshake: Verified." -ForegroundColor Green
} else {
    Write-Host "[WARN] Missing API Keys. What is 'Neural Fragmentation'?" -ForegroundColor Yellow
}

# 3. Hardware Resonance (ADB & IP Detection)
Write-Host "`n--- [HARDWARE] Probing Physical Resonance ---" -ForegroundColor Yellow
if (Get-Command adb -ErrorAction SilentlyContinue) {
    $devices = adb devices | Select-String -Pattern "\tdevice$"
    if ($devices) {
        # Detect Device Model and Architecture for "Proper Model" configuration
        $deviceModel = adb shell getprop ro.product.model
        $deviceAbi = adb shell getprop ro.product.cpu.abi
        Write-Host "[DETECTED] Device: $deviceModel ($deviceAbi)" -ForegroundColor Cyan

        $ipInfo = adb shell ip route | Select-String "src\s+([0-9.]+)"
        if ($ipInfo -match "src\s+([0-9.]+)") {
            $deviceIp = $Matches[1]
            Write-Host "[READY] Android Host detected at $deviceIp" -ForegroundColor Green
        } else {
            $deviceIp = "localhost"
            Write-Host "[READY] Android Host detected via Loopback" -ForegroundColor Gray
        }
    } else {
        Write-Host "[OFFLINE] No Android device detected. Is the Program connected?" -ForegroundColor Red
    }
}

# 4. Python Sync Engine Activation
if (-not $SkipSync) {
    Write-Host "`n--- [OVERSEER] Synchronizing Gemini Oversight ---" -ForegroundColor Yellow
    
    $pythonExe = Join-Path $ScriptDir ".venv\Scripts\python.exe"
    if (-not (Test-Path $pythonExe)) {
        $pythonExe = Join-Path $ScriptDir ".venv\bin\python.exe"
    }
    if (-not (Test-Path $pythonExe)) {
        $pythonExe = Join-Path $ScriptDir ".venv\bin\python"
    }
    if (-not (Test-Path $pythonExe)) {
        $pythonExe = "python"
    }
    
    if (Get-Command $pythonExe -ErrorAction SilentlyContinue) {
        Write-Host "Updating Python dependencies..." -ForegroundColor Gray
        & $pythonExe -m pip install requests --quiet
        
        Write-Host "[EXEC] Launching Sync Engine on port $($WebPort)..." -ForegroundColor Cyan
        $argList = @("$syncEnginePath")
        if ($deviceIp) { $argList += "--ip"; $argList += "$deviceIp" }
        
        # Start in a separate process to keep the grid alive
        Start-Process $pythonExe -ArgumentList $argList -NoNewWindow
    } else {
        Write-Host "[ERROR] Python is dark. Sync Engine cannot engage." -ForegroundColor Red
    }
}

# 5. Be Kind Rewind (Optional Reset)
if ($Refresh) {
    Write-Host "`n--- [ADMIN] What is 'Sweding the Grid'? ---" -ForegroundColor Magenta
    Write-Host "Purging local caches and recycling Port $($WebPort)..." -ForegroundColor Gray
    $portCheck = Get-NetTCPConnection -LocalPort $WebPort -ErrorAction SilentlyContinue
    if ($portCheck) {
        Stop-Process -Id $portCheck.OwningProcess -Force
        Write-Host "[OK] Port $($WebPort) recycled." -ForegroundColor Green
    }
    
    if ($deviceIp -and $deviceIp -ne "localhost") {
        Write-Host "Re-initializing Android Host via ADB..." -ForegroundColor Gray
        adb shell am force-stop com.example.djmidiwatts
        adb shell am start -n com.example.djmidiwatts/com.example.djmidiwatts.MainActivity | Out-Null
    }
    $argList += "--port"; $argList += "$WebPort" # Pass port to Python script
}

# 6. Final Status
Write-Host "`n--- [SYSTEM] Grid Activation Complete on Port $($WebPort) ---" -ForegroundColor Cyan
$portCheck = Get-NetTCPConnection -LocalPort $WebPort -ErrorAction SilentlyContinue
if ($portCheck) {
    Write-Host "[ONLINE] Bridge active on Port $($WebPort). May the Force be with your faders." -ForegroundColor Green
} else {
    Write-Host "[STANDBY] Activation successful, awaiting Android Host handshake..." -ForegroundColor Yellow
}

Write-Host "`nReady for Operative input." -ForegroundColor Gray