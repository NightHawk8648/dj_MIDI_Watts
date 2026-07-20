# DJ MIDI WATTS - Global Synchronicity & Format Health Diagnostic
# This script verifies the health and connectivity of all project formats.
# Triggered manually for troubleshooting, testing, or update verification.
param(
    [switch]$Repair,
    [switch]$Silent
)

$repoRoot = Resolve-Path "${PSScriptRoot}\.."
$hostUrl = "http://localhost:8080"
$buildScript = Join-Path $repoRoot "scripts\build_and_launch.ps1"

function Start-RepairCycle {
    Write-Host "`n[REPAIR] Initializing Build & Replace Loop..." -ForegroundColor Yellow
    
    # Maintenance Request
    $doMaintenance = $false
    $isNonInteractive = -not [Environment]::UserInteractive
    
    if ($Silent -or $isNonInteractive) {
        $doMaintenance = $true
        if ($isNonInteractive -and -not $Silent) {
            Write-Host "  - Non-interactive shell detected. Defaulting to silent mode." -ForegroundColor Gray
        }
    }
    else {
        $response = Read-Host "Would you like to perform a maintenance cleanup? (Remove temporary files, old logs, and unused libs) [Y/N]"
        if ($response -eq 'Y' -or $response -eq 'y') {
            $doMaintenance = $true
        }
    }

    if ($doMaintenance) {
        Write-Host "  - Executing system maintenance..." -ForegroundColor Cyan
        
        # 1. Temporary Files (Build artifacts)
        $buildPath = Join-Path $repoRoot "build"
        if (Test-Path $buildPath) {
            Write-Host "    - Purging temporary build directory..." -ForegroundColor Gray
            Remove-Item -Path $buildPath -Recurse -Force -ErrorAction SilentlyContinue
        }

        # 2. Old Logs
        $logPath = Join-Path $repoRoot "logs"
        if (Test-Path $logPath) {
            Write-Host "    - Clearing log history..." -ForegroundColor Gray
            Remove-Item -Path "$logPath\*" -Include *.log, *.txt -Force -ErrorAction SilentlyContinue
        }

        # 3. Unused Libs
        $libPath = Join-Path $repoRoot "libs"
        if (Test-Path $libPath) {
            Write-Host "    - Removing library cache..." -ForegroundColor Gray
            Remove-Item -Path "$libPath\*" -Recurse -Force -ErrorAction SilentlyContinue
        }
        Write-Host "  - Maintenance complete." -ForegroundColor Green
    }

    $distPath = Join-Path $repoRoot "dist"
    if (Test-Path $distPath) {
        Write-Host "  - Clearing stale artifacts from $distPath..." -ForegroundColor Gray
        Remove-Item -Path "$distPath\*" -Recurse -Force -ErrorAction SilentlyContinue
    }

    # 4. Dependency Restoration
    Write-Host "  - Restoring project dependencies..." -ForegroundColor Cyan
    
    # Python Dependencies
    if (Test-Path (Join-Path $repoRoot "requirements.txt")) {
        Write-Host "    - Syncing Python requirements..." -ForegroundColor Gray
        $pipExec = "$repoRoot\.venv\Scripts\pip.exe"
        if (-not (Test-Path $pipExec)) {
            $pipExec = "$repoRoot\.venv\bin\pip.exe"
        }
        if (-not (Test-Path $pipExec)) {
            $pipExec = "$repoRoot\.venv\bin\pip"
        }
        if (-not (Test-Path $pipExec)) {
            $pipExec = "pip"
        }
        & $pipExec install -r (Join-Path $repoRoot "requirements.txt") --quiet --no-input
    }

    # Flutter/Mobile Dependencies
    if (Get-Command flutter -ErrorAction SilentlyContinue) {
        $flutterPath = if (Test-Path "$repoRoot\pubspec.yaml") { $repoRoot } else { Join-Path $repoRoot "app" }
        if (Test-Path $flutterPath) {
            Write-Host "    - Running flutter pub get in $flutterPath..." -ForegroundColor Gray
            Push-Location $flutterPath
            & flutter pub get | Out-Null
            Pop-Location
        }
    }

    # Web/Node Dependencies (WebUI and Extension)
    $webDirs = @(Join-Path $repoRoot "webui", Join-Path $repoRoot "extension")
    foreach ($dir in $webDirs) {
        if (Test-Path (Join-Path $dir "package.json")) {
            Write-Host "    - Running npm install in $(Split-Path $dir -Leaf)..." -ForegroundColor Gray
            Push-Location $dir
            & npm install --quiet --no-audit --no-fund
            Pop-Location
        }
    }

    # Executes the build script without launching a final target to refresh artifacts
    powershell.exe -File $buildScript
}

Write-Host "`n====================================================" -ForegroundColor Cyan
Write-Host "   DJ MIDI WATTS: GLOBAL SYNCHRONICITY CHECK" -ForegroundColor Cyan
Write-Host "====================================================`n" -ForegroundColor Cyan

# 1. MOBILE FORMAT (Android - Kotlin/Dart)
Write-Host "[FORMAT] MOBILE (Android/Flutter)" -ForegroundColor Magenta
$mobileHealthy = $true
if (Get-Command flutter -ErrorAction SilentlyContinue) {
    Write-Host "  - SDK: Flutter detected." -ForegroundColor Green
}
else {
    $mobileHealthy = $false
}
if (Get-Command adb -ErrorAction SilentlyContinue) {
    $devices = adb devices | Select-String -Pattern "\tdevice$"
    if ($devices) {
        Write-Host "  - Connectivity: ADB Active ($($devices.Count) device(s) linked)." -ForegroundColor Green
    }
    else {
        Write-Warning "  - Connectivity: ADB running but no devices found."
    }
}
else {
    $mobileHealthy = $false
    Write-Error "  - Connectivity: ADB (Android Debug Bridge) not found."
}
[void]$mobileHealthy

# 2. WEB FORMAT (WebUI / UI Hub)
Write-Host "`n[FORMAT] WEB (HTML5/JavaScript)" -ForegroundColor Magenta
try {
    $null = Invoke-WebRequest -Uri $hostUrl -UseBasicParsing -TimeoutSec 2
    Write-Host "  - Hub: WebUI reachable at $hostUrl" -ForegroundColor Green
}
catch {
    Write-Warning "  - Hub: WebUI offline. Local synchronization hub is not responding."
}

# 3. EXTENSION FORMAT (Browser Sync)
Write-Host "`n[FORMAT] EXTENSION (manifest-v3)" -ForegroundColor Magenta
$extPath = Join-Path $repoRoot "dj-midi-watts-extension"
$extHealthy = $true
if (Test-Path (Join-Path $extPath "manifest.json")) {
    Write-Host "  - State: Extension package valid." -ForegroundColor Green
}
else {
    Write-Error "  - State: manifest.json missing in extension directory."
}

# 4. DESKTOP FORMAT (Kotlin/Java/EXE)
Write-Host "`n[FORMAT] DESKTOP (Native Windows)" -ForegroundColor Magenta
# Default naming from launch.json
$jarPath = Join-Path $repoRoot "build\libs\dj_MIDI_Watts.jar"
$exePath = Join-Path $repoRoot "dist\dj_MIDI_Watts.exe"
$desktopHealthy = $true

if (Test-Path $jarPath) {
    Write-Host "  - Artifact: Java JAR found." -ForegroundColor Green
}
if (Test-Path $exePath) {
    Write-Host "  - Artifact: Native EXE found." -ForegroundColor Green
}
else {
    $desktopHealthy = $false
    Write-Warning "  - Artifact: Desktop binary missing. Sync check partial."
}

# Trigger Repair Loop if any artifacts are missing and -Repair was requested
if ($Repair -and (-not $desktopHealthy -or -not $extHealthy)) {
    Start-RepairCycle
    Write-Host "`n[REPAIR] Cycle Complete. Re-verifying artifacts..." -ForegroundColor Cyan
}

# 5. REAL-TIME SYNCHRONIZATION TELEMETRY
Write-Host "`n[SYNC] GLOBAL HUB TELEMETRY" -ForegroundColor Yellow
try {
    $state = Invoke-RestMethod -Uri "$hostUrl/api/state" -Method Get
    $isSynced = $state.isMidiHardwareConnected -and ($state.ai_active -ne $false)
    
    if ($isSynced) {
        Write-Host "  - Status: FULLY SYNCHRONIZED" -ForegroundColor Green
        Write-Host "  - Active Track: $($state.current_track)" -ForegroundColor Gray
        Write-Host "  - Resonance Color: $($state.theme_color)" -ForegroundColor Gray
    }
    else {
        Write-Warning "  - Status: PARTIAL SYNC (Verify MIDI Hardware or AI S1 Link)."
    }
}
catch {
    Write-Error "  - Telemetry: HUB API unreachable. Synchronized rendering is currently disabled."
}

Write-Host "`n================ CHECK COMPLETE ================`n" -ForegroundColor Cyan
