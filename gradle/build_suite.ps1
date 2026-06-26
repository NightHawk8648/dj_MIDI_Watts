# DJ MIDI WATTS - Ultima-Grid Build Suite Orchestrator
# "What is 'Grid Deployment', Alex?"

param (
    [switch]$Reveal,
    [switch]$Offline,
    [int]$VoiceIndex = -1
)

$ErrorActionPreference = "Stop"

function Get-MaskedValue ($Value) {
    if (-not $Value -or $Value -match "VOID") { return "NOT_SET/VOID" }
    if ($Reveal) { return $Value }
    if ($Value.Length -gt 8) {
        return $Value.Substring(0, 4) + "..." + $Value.Substring($Value.Length - 4)
    }
    return "********"
}

Write-Host "--- [CHAIN OF COMMAND] Initiating Tier-Based Deployment ---" -ForegroundColor Cyan

try {
    # TIER 3 & 4: Administrator & Translator Handshake (Diagnostics)
    Write-Host "`n[DIAGNOSTIC] Consulting the Administrator and Translator..." -ForegroundColor Yellow
    $diagPath = Join-Path $PSScriptRoot "../foo.ps1"
    $diagArgs = @("-RunTests")
    if ($Reveal) { $diagArgs += "-Reveal" }
    if ($Offline) { $diagArgs += "-Offline" }
    powershell -ExecutionPolicy Bypass -File $diagPath $diagArgs

    # TIER 4: Translator Protocol (Web UI Build) - What is 'Node Package Mastery'?
    Write-Host "`n[TRANSLATOR] Compiling Web UI Commander..." -ForegroundColor Magenta
    $webUiPath = Join-Path $PSScriptRoot "../web-ui"
    $packageJsonPath = Join-Path $webUiPath "package.json"

    if (Test-Path $packageJsonPath) {
        $packageJson = Get-Content $packageJsonPath -Raw | ConvertFrom-Json
        $buildScript = $packageJson.scripts.build
        $placeholder = "echo 'Placeholder build: Web UI assets ready.'"

        if ($buildScript -eq $placeholder) {
            Write-Host "[INFO] Placeholder build script detected. Skipping Web UI compilation to maintain Grid speed." -ForegroundColor Gray
        } else {
            Push-Location $webUiPath
            if (-not (Test-Path "node_modules")) {
                Write-Host "Reclaiming dependencies..." -ForegroundColor Gray
                npm install
            }
            npm run build
            Pop-Location
            Write-Host "[OK] Translator: Web assets synchronized and optimized." -ForegroundColor Green
        }
    } else {
        Write-Host "[WARN] Translator source missing (package.json). Skipping Web UI compilation." -ForegroundColor Yellow
    }

    # TIER 0: Master Administrator Execution (Android Gradle Build)
    Write-Host "`n[MASTER ADMIN] Assembling the Grid... 1.21 Gigawatts standing by!" -ForegroundColor Red
    
    # Check for S1/S2 burial in environment or local.properties for Gradle
    $s1 = if ($env:UG_S1) { $env:UG_S1 } else { $env:GEMINI_API_KEY }
    $s2 = $env:UG_S2
    $localProps = Join-Path $PSScriptRoot "../local.properties"

    if (-not $s1 -or -not $s2) {
        try {
            $vault = New-Object Windows.Security.Credentials.PasswordVault
            if (-not $s1) { $s1 = $vault.Retrieve("UltimaGrid", "UG_S1").Password }
            if (-not $s2) { $s2 = $vault.Retrieve("UltimaGrid", "UG_S2").Password }
        } catch {
            # Vault check failed, proceed to local.properties
        }
    }

    if ((-not $s1 -or -not $s2) -and (Test-Path $localProps)) {
        $props = Get-Content $localProps
        foreach ($line in $props) {
            if (-not $s1 -and ($line -match "UG_S1=(.*)")) { $s1 = $Matches[1].Trim().Trim('"') }
            if (-not $s2 -and ($line -match "UG_S2=(.*)")) { $s2 = $Matches[1].Trim().Trim('"') }
        }
    }

    if ((-not $s1 -or $s1 -match "VOID" -or -not $s2 -or $s2 -match "VOID") -and -not $Offline) {
        throw "Chain of Command Failure: Layer S1/S2 missing or set to VOID. The Grid is dark."
    }

    Write-Host "[OK] Credentials Verified for Assembly:" -ForegroundColor Green
    Write-Host " -> S1: $(Get-MaskedValue $s1)" -ForegroundColor Gray
    Write-Host " -> S2: $(Get-MaskedValue $s2)" -ForegroundColor Gray

    $gradlew = if ($IsWindows) { ".\gradlew.bat" } else { "./gradlew" }
    Push-Location (Join-Path $PSScriptRoot "..")
    
    Write-Host "Cleaning Matrix caches..." -ForegroundColor Gray
    & $gradlew clean

    Write-Host "Executing Tier-0 Assembly..." -ForegroundColor Gray
    & $gradlew assembleDebug

    Pop-Location

    Write-Host "`n--- [SUCCESS] Tournament Build Ready ---" -ForegroundColor Cyan
    Write-Host "Master Admin: 'The Force is strong with this build.'" -ForegroundColor Green
    
    # Audio Handshake: Tier-0 Assembly Confirmation
    $voice = New-Object -ComObject SAPI.SpVoice
    $voices = $voice.GetVoices()
    if ($VoiceIndex -ge 0 -and $VoiceIndex -lt $voices.Count) {
        $voice.Voice = $voices.Item($VoiceIndex)
    } else {
        # Fallback to Zira search if no valid index provided
        $voice.Voice = $voices | Where-Object { $_.GetDescription() -like "*Zira*" } | Select-Object -First 1
    }
    $voice.Rate = -2 # Methodical, noir pacing
    $voice.Speak("Tournament build ready. The force is strong with this build.")

    # Locate Resulting APK
    $apkPath = Resolve-Path (Join-Path $PSScriptRoot "../app/build/outputs/apk/debug/*.apk")
    Write-Host "Deployment Unit: $apkPath" -ForegroundColor Gray

} catch {
    Write-Host "`n[CRITICAL FAILURE] I've got a bad feeling about this..." -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)"
    exit 1
}