param (
    [switch]$Offline,
    [int]$VoiceIndex = -1
)

$ErrorActionPreference = "Stop"

try {
    $deviceIp = "localhost"

    # Initialize Neural Voice (Noir Configuration)
    $voice = New-Object -ComObject SAPI.SpVoice
    $voices = $voice.GetVoices()
    if ($VoiceIndex -ge 0 -and $VoiceIndex -lt $voices.Count) {
        $voice.Voice = $voices.Item($VoiceIndex)
    } else {
        # Fallback logic
        $voice.Voice = $voices | Where-Object { $_.GetDescription() -like "*Zira*" } | Select-Object -First 1
    }
    $voice.Rate = -2

    if (-not $Offline) {
        Write-Host "--- [ADB AUTO-SYNC] Detecting Device ---" -ForegroundColor Yellow
        
        # Check if any devices are connected
        $devices = adb devices | Select-String -Pattern "\tdevice$"
        if ($null -eq $devices) {
            throw "No ADB devices found. Please connect your device via USB or ensure Wireless Debugging is active."
        }

        Write-Host "Fetching IP address from device..." -ForegroundColor Gray
        
        # Try getting IP from wlan0 interface first
        $ipInfo = adb shell ip -f inet addr show wlan0

        if ($ipInfo -match "inet\s+([0-9.]+)") {
            $deviceIp = $Matches[1]
        } else {
            # Fallback to ip route to find the source IP on the local subnet
            $routeInfo = adb shell ip route | Select-String "src\s+([0-9.]+)"
            if ($routeInfo -match "src\s+([0-9.]+)") {
                $deviceIp = $Matches[1]
            }
        }

        if ([string]::IsNullOrWhiteSpace($deviceIp)) {
            throw "Could not determine Device IP address. Ensure Wi-Fi is connected on the device."
        }

        Write-Host "Success! Target Device IP: $deviceIp" -ForegroundColor Green
        $voice.Speak("Grid synchronization established. Welcome back, Operative.")
    } else {
        Write-Host "--- [OFFLINE] Bypassing hardware resonance. Simulating grid logic... ---" -ForegroundColor Cyan
        $voice.Speak("Simulated grid logic engaged. Offline mode active.")
    }

    # Determine Port from local.properties if possible
    $webPort = 8081
    $localPropsPath = Join-Path $PSScriptRoot "../app/local.properties"
    if (Test-Path $localPropsPath) {
        $portLine = Get-Content $localPropsPath | Where-Object { $_ -match "WEB_PORT=(\d+)" }
        if ($portLine -match "WEB_PORT=(\d+)") {
            $webPort = $Matches[1]
            Write-Host "Detected WEB_PORT from local.properties: $webPort" -ForegroundColor Gray
        }
    }
    
    # Locate the sync engine - checks scripts folder first, then the package source
    $enginePath = Join-Path $PSScriptRoot "sync_engine.py"
    if (-not (Test-Path $enginePath)) { 
        $enginePath = Join-Path $PSScriptRoot "../app/src/main/java/com/example/ui/sync_engine.py" 
    }

    $pyArgs = @($enginePath)
    if ($Offline) {
        $pyArgs += "--offline"
    } else {
        $pyArgs += "--ip", $deviceIp
        $pyArgs += "--port", $webPort
    }

    $pythonExe = Join-Path $PSScriptRoot "../.venv/Scripts/python.exe"
    if (-not (Test-Path $pythonExe)) {
        $pythonExe = Join-Path $PSScriptRoot "../.venv/bin/python.exe"
    }
    if (-not (Test-Path $pythonExe)) {
        $pythonExe = Join-Path $PSScriptRoot "../.venv/bin/python"
    }
    if (-not (Test-Path $pythonExe)) {
        $pythonExe = "python"
    }
    & $pythonExe $pyArgs
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}