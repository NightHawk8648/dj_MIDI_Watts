## DJ MIDI WATTS – CLI Integration (updated with CI wrappers)
# This PowerShell script provides a simple command‑line workflow for the project.
#   provision        – store Gemini & OAuth keys in the user env + vault
#   clean            – run Gradle clean
#   test             – run unit tests
#   buildDebug       – './gradle-9.3.1/bin/gradle assembleDebug'
#   buildRelease     – './gradle-9.3.1/bin/gradle assembleRelease' (signing config required)
#   install          – install the generated debug APK on a connected device/emulator
#   deprovision      – clean up stored secrets
#   printSecrets     – display environment variables via the custom Gradle task
#   ci               – CI‑pipeline shortcut (clean → test → buildDebug)
#
# Usage examples (run from the project root):
#   .\scripts\cli_integration.ps1 provision <GeminiKey> <OAuthClientId>
#   .\scripts\cli_integration.ps1 clean
#   .\scripts\cli_integration.ps1 test
#   .\scripts\cli_integration.ps1 buildDebug
#   .\scripts\cli_integration.ps1 ci
#   .\scripts\cli_integration.ps1 deprovision
#
param (
    [Parameter(Position = 0, Mandatory = $true)]
    [ValidateSet('provision','clean','test','buildDebug','buildRelease','install','deprovision','printSecrets','ci','agent-setup','agent-assemble','agent-debug','agent-build')]
    [string]$Command,

    # Only required for the 'provision' command
    [string]$GeminiKey,
    [string]$OAuthClientId
)

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
# Move up one level to the project root
$ProjectRoot = Resolve-Path (Join-Path $ProjectRoot '..')
$GradleWrapper = Join-Path $ProjectRoot 'gradlew.bat'   # use the wrapper if present
$GradleBin = Join-Path $ProjectRoot 'gradle-9.3.1\bin\gradle.bat'  # fallback to downloaded binary
$GradleCmd = if (Test-Path $GradleWrapper) { $GradleWrapper } else { $GradleBin }

$ProvisionScript   = Join-Path $ProjectRoot 'scripts\provision_credentials.ps1'
$DeprovisionScript = Join-Path $ProjectRoot 'scripts\deprovision_credentials.ps1'

# ----- Command implementations --------------------------------------
function Invoke-Provision {
    if (-not $GeminiKey -or -not $OAuthClientId) {
        Write-Error "Provision requires both <GeminiKey> and <OAuthClientId>."
        exit 1
    }
    Write-Host "▶️ Provisioning credentials…" -ForegroundColor Cyan
    & $ProvisionScript -S1_Key $GeminiKey -S2_Key $OAuthClientId
    if ($LASTEXITCODE -ne 0) { Write-Error "Provision failed."; exit $LASTEXITCODE }
    Write-Host "✅ Provisioning complete." -ForegroundColor Green
}

function Invoke-Clean {
    Write-Host "▶️ Running Gradle clean…" -ForegroundColor Cyan
    & $GradleCmd clean
    if ($LASTEXITCODE -ne 0) { Write-Error "Gradle clean failed."; exit $LASTEXITCODE }
    Write-Host "✅ Clean successful." -ForegroundColor Green
}

function Invoke-Test {
    Write-Host "▶️ Running unit tests…" -ForegroundColor Cyan
    & $GradleCmd test
    if ($LASTEXITCODE -ne 0) { Write-Error "Tests failed."; exit $LASTEXITCODE }
    Write-Host "✅ All tests passed." -ForegroundColor Green
}

function Invoke-BuildDebug {
    Write-Host "▶️ Assembling Debug APK (with K2 LightTree fix)…" -ForegroundColor Cyan
    # Passing -Xuse-fir-lt=false via Gradle properties to suppress K2 script warnings
    & $GradleCmd assembleDebug -Pkotlin.experimental.tryK2=false
    if ($LASTEXITCODE -ne 0) { Write-Error "Debug build failed."; exit $LASTEXITCODE }
    Write-Host "✅ Debug APK built successfully." -ForegroundColor Green
}

function Invoke-BuildRelease {
    Write-Host "▶️ Assembling Release APK…" -ForegroundColor Cyan
    & $GradleCmd assembleRelease
    if ($LASTEXITCODE -ne 0) { Write-Error "Release build failed."; exit $LASTEXITCODE }
    Write-Host "✅ Release APK built successfully." -ForegroundColor Green
}

function Invoke-Install {
    Write-Host "▶️ Select Deployment Target Format:" -ForegroundColor Cyan
    Write-Host "[0] Android APK (Mobile/Emulator)"
    Write-Host "[1] Chrome Extension (Unpacked)"
    Write-Host "[2] Python Antigravity SDK (Skills/Agent)"

    Write-Host "`nEnter format index [0-2] (Defaulting to Android [0] in 60s): " -NoNewline
    
    $formatSelection = 0
    $timeout = 60
    $timer = [Diagnostics.Stopwatch]::StartNew()
    
    while ($timer.Elapsed.TotalSeconds -lt $timeout) {
        if ([Console]::KeyAvailable) {
            $input = Read-Host
            if ($input -match '^[0-2]$') {
                $formatSelection = [int]$input
                break
            }
            Write-Host "Invalid selection. Enter 0, 1, or 2: " -NoNewline
        }
        Start-Sleep -Milliseconds 100
    }

    switch ($formatSelection) {
        0 { Install-Android }
        1 { Install-Extension }
        2 { Install-PythonSDK }
    }
}

function Install-Extension {
    $ExtPath = Join-Path $ProjectRoot 'dj-midi-watts-extension'
    Write-Host "`n📦 Chrome Extension Path: $ExtPath" -ForegroundColor Green
    Write-Host "ℹ️ To install: Open chrome://extensions, enable 'Developer mode', and 'Load unpacked'." -ForegroundColor Gray
}

function Install-PythonSDK {
    $SDKPath = Join-Path $ProjectRoot 'antigravity-sdk-python'
    Write-Host "`n🐍 Installing Python SDK and Skill dependencies..." -ForegroundColor Cyan
    if (Test-Path $SDKPath) {
        Set-Location $SDKPath
        python -m pip install -e .
        Write-Host "✅ SDK linked in editable mode." -ForegroundColor Green
        Set-Location $ProjectRoot
    } else {
        Write-Error "SDK path not found at $SDKPath"
    }
}

function Install-Android {
    Write-Host "`n📱 Preparing Android installation..." -ForegroundColor Cyan

    # Get list of devices via ADB
    $devices = adb devices | Select-String -Pattern "\tdevice$" | ForEach-Object { $_.ToString().Split("`t")[0] }

    if (-not $devices) {
        Write-Error "No devices detected via ADB. Ensure your device is connected and USB debugging is enabled."
        exit 1
    }

    $selectedDevice = $null

    if ($devices.Count -eq 1) {
        $selectedDevice = $devices[0]
        Write-Host "ℹ️ Single device detected: $selectedDevice" -ForegroundColor Gray
    } else {
        Write-Host "Available Devices:" -ForegroundColor Yellow
        for ($i = 0; $i -lt $devices.Count; $i++) {
            Write-Host "[$i] $($devices[$i])"
        }

        Write-Host "`nPlease enter the device index [0-$($devices.Count-1)] (Defaulting to [0] in 60s): " -NoNewline
        
        $timeout = 60
        $timer = [Diagnostics.Stopwatch]::StartNew()
        
        while ($timer.Elapsed.TotalSeconds -lt $timeout) {
            if ([Console]::KeyAvailable) {
                $input = Read-Host
                if ($input -match '^\d+$' -and [int]$input -lt $devices.Count) {
                    $selectedDevice = $devices[[int]$input]
                    break
                }
                Write-Host "Invalid selection. Enter index [0-$($devices.Count-1)]: " -NoNewline
            }
            Start-Sleep -Milliseconds 100
        }

        if ($null -eq $selectedDevice) {
            Write-Host "`n⏳ Timeout reached. Auto-scanning for primary device..." -ForegroundColor Yellow
            $selectedDevice = $devices[0]
        }
    }

    Write-Host "▶️ Installing Debug APK on $selectedDevice..." -ForegroundColor Cyan
    $env:ANDROID_SERIAL = $selectedDevice
    & $GradleCmd installDebug
    if ($LASTEXITCODE -ne 0) { Write-Error "Installation failed."; exit $LASTEXITCODE }
    Write-Host "✅ Installation complete on $selectedDevice." -ForegroundColor Green
}

function Invoke-Deprovision {
    Write-Host "▶️ Removing stored credentials…" -ForegroundColor Cyan
    & $DeprovisionScript
    if ($LASTEXITCODE -ne 0) { Write-Error "De‑provision failed."; exit $LASTEXITCODE }
    Write-Host "✅ Credentials removed." -ForegroundColor Green
}

function Invoke-PrintSecrets {
    Write-Host "▶️ Printing loaded environment variables…" -ForegroundColor Cyan
    & $GradleCmd printSecrets
}

function Invoke-CI {
    Write-Host "▶️ CI pipeline: clean → test → buildDebug" -ForegroundColor Cyan
    Invoke-Clean
    Invoke-Test
    Invoke-BuildDebug
}

function Invoke-AgentSetup {
    Write-Host "▶️ Launching Interactive Agent Manager & Setup..." -ForegroundColor Cyan
    $pythonExe = Join-Path $ProjectRoot ".venv\Scripts\python.exe"
    if (-not (Test-Path $pythonExe)) {
        $pythonExe = Join-Path $ProjectRoot ".venv\bin\python.exe"
    }
    if (-not (Test-Path $pythonExe)) {
        $pythonExe = Join-Path $ProjectRoot ".venv\bin\python"
    }
    if (-not (Test-Path $pythonExe)) {
        $pythonExe = "python"
    }
    & $pythonExe (Join-Path $ProjectRoot 'agents\agent_manager.py')
}

function Invoke-AgentAssemble {
    & (Join-Path $ProjectRoot 'agents\build_agents.ps1') assemble
}

function Invoke-AgentDebug {
    & (Join-Path $ProjectRoot 'agents\build_agents.ps1') debug
}

function Invoke-AgentBuild {
    & (Join-Path $ProjectRoot 'agents\build_agents.ps1') build
}

# ----- Dispatch ------------------------------------------------------
switch ($Command) {
    'provision'    { Invoke-Provision }
    'clean'         { Invoke-Clean }
    'test'          { Invoke-Test }
    'buildDebug'    { Invoke-BuildDebug }
    'buildRelease'  { Invoke-BuildRelease }
    'install'       { Invoke-Install }
    'deprovision'   { Invoke-Deprovision }
    'printSecrets'  { Invoke-PrintSecrets }
    'ci'            { Invoke-CI }
    'agent-setup'   { Invoke-AgentSetup }
    'agent-assemble'{ Invoke-AgentAssemble }
    'agent-debug'   { Invoke-AgentDebug }
    'agent-build'   { Invoke-AgentBuild }
    default {
        Write-Error "Unknown command $Command"
        exit 1
    }
}
