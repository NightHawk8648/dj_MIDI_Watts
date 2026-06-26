# PowerShell script: build_and_launch.ps1
# ------------------------------------------------------------
# Integrates Android app, Web UI, and Web Extension builds.
# Loads .env variables, activates Python virtual environment (.venv),
# runs branch‑specific commands, and finally launches the configured target.
# ------------------------------------------------------------

# Resolve repository root (parent of the scripts folder)
$repoRoot = Resolve-Path "${PSScriptRoot}\.."

# -----------------------------------------------------------------
# Load .env if it exists (key=value per line)
# -----------------------------------------------------------------
$envFile = Join-Path $repoRoot ".env"
if (Test-Path $envFile) {
    Write-Host "Loading .env variables" -ForegroundColor Cyan
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^([^#=]+)=(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
}
else {
    Write-Warning ".env file not found – proceeding with defaults."
}

# -----------------------------------------------------------------
# Activate Python virtual environment if present
# -----------------------------------------------------------------
$venvActivate = Join-Path $repoRoot ".venv\Scripts\Activate.ps1"
if (Test-Path $venvActivate) {
    Write-Host "Activating .venv" -ForegroundColor Cyan
    & $venvActivate
}
else {
    Write-Warning "Python virtual environment (.venv) not found – skipping activation."
}

# -----------------------------------------------------------------
# Load branch configuration (branch_config.json)
# -----------------------------------------------------------------
# Verify required drivers/hardware for heavy graphics, Android, etc.
# -----------------------------------------------------------------
function Check-RequiredDrivers {
    param($cfg)
    $missing = @()
    $videoControllers = Get-WmiObject Win32_VideoController
    if (-not $videoControllers) { $missing += "GPU driver" }
    if (-not (Get-Command dxdiag -ErrorAction SilentlyContinue)) { $missing += "DirectX" }
    if (-not (Test-Path "C:\\VulkanSDK")) { $missing += "Vulkan SDK" }
    if ($cfg -and $cfg.launchConfig -match "Android" -and -not (Get-Command adb -ErrorAction SilentlyContinue)) { $missing += "ADB" }
    
    if ($missing.Count -gt 0) {
        Write-Error "Missing required drivers/services: $($missing -join ', ')"
        exit 1
    }
    Write-Host "All required drivers detected." -ForegroundColor Green
}

function Invoke-CommandInDir {
    param(
[string]$Command,
[string]$WorkingDir = $repoRoot
)
Push-Location $WorkingDir
Write-Host "Running in $WorkingDir: $Command" -ForegroundColor Yellow
Invoke-Expression $Command
$exit = $LASTEXITCODE
Pop-Location
if ($exit -ne 0) {
    Write-Error "Command failed (exit code $exit)"
    exit $exit
}
}

# -----------------------------------------------------------------
# Execute build and launch steps
# -----------------------------------------------------------------
Check-RequiredDrivers $cfg

if ($null -ne $cfg.buildCommand) {
    Invoke-CommandInDir $cfg.buildCommand $repoRoot
}

if ($null -ne $cfg.webuiBuild) {
    $webuiDir = Join-Path $repoRoot "webui"
    if (Test-Path $webuiDir) {
        Invoke-CommandInDir $cfg.webuiBuild $webuiDir
    }
    else {
        Write-Warning "Web UI directory not found – skipping web UI build."
    }
}

if ($null -ne $cfg.extensionBuild) {
    $extDir = Join-Path $repoRoot "extension"
    if (Test-Path $extDir) {
        Invoke-CommandInDir $cfg.extensionBuild $extDir
    }
    else {
        Write-Warning "Web extension directory not found – skipping extension build."
    }
}

# -----------------------------------------------------------------
# Launch target (URL or VS Code launch configuration name)
# -----------------------------------------------------------------
if ($null -ne $cfg.url) {
    Write-Host "Opening URL: $($cfg.url)" -ForegroundColor Green
    Start-Process $cfg.url
}
elseif ($null -ne $cfg.launchConfig) {
    $finalConfig = $cfg.launchConfig
    # Conditional logic to choose between C++ Extension or Node (pwa-node) for .exe launch
    if ($finalConfig -eq "Run Executable") {
        # Verify if the executable exists before proceeding
        $exeName = if ($null -ne $cfg.exeName) { $cfg.exeName } else { "dj_MIDI_Watts" }
        $exePath = Join-Path $repoRoot "dist\$exeName.exe"

        if (-not (Test-Path $exePath)) {
            Write-Error "Target executable not found: $exePath. Ensure the build process finished correctly."
            exit 1
        }

        if ($env:DEBUG_MODE -eq "true") {
            Write-Host "Debugging detected: Switching to C++ Debugger extension." -ForegroundColor Cyan
            $finalConfig = "Debug Executable (C++)"
        }
        else {
            Write-Host "Standard run: Utilizing pwa-node to launch the executable." -ForegroundColor Cyan
        }
    }
    elseif ($finalConfig -match "Build & Launch") {
        # Verify if the JAR exists (commonly in build/libs for Gradle projects)
        $jarName = if ($null -ne $cfg.jarName) { $cfg.jarName } else { "dj_MIDI_Watts" }
        $jarPath = Join-Path $repoRoot "build/libs/$jarName.jar"

        if (-not (Test-Path $jarPath)) {
            Write-Warning "Target JAR not found: $jarPath. The Java process may attempt to run from class files instead."
        }
    }

    Write-Host "Requested VS Code launch config: $finalConfig" -ForegroundColor Green
    Write-Output "LAUNCH_CONFIG=$finalConfig"
}
