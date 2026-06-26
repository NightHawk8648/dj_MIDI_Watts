# Ultima-Grid Build & Pre-Build Suite for Windows
Write-Host "--- [ULTIMA-GRID] Starting Build Orchestration ---" -ForegroundColor Cyan

function Check-Env {
    param($cmd, $name)
    if (Get-Command $cmd -ErrorAction SilentlyContinue) {
        Write-Host "[CHECK] $name found." -ForegroundColor Green
    } else {
        Write-Host "[ERROR] $name not found. Please install before proceeding." -ForegroundColor Red
        exit 1
    }
}

# 1. Pre-build Environment Check
Write-Host "`n[STEP 1] Running Pre-build environment checks..." -ForegroundColor Yellow
Check-Env "java" "Java SDK"
Check-Env "npm" "Node Package Manager"
Check-Env "python" "Python 3"
Check-Env "gradlew" "Gradle Wrapper"

# 2. GUI Rendering & Web UI Build
Write-Host "`n[STEP 2] Executing GUI Rendering Pipeline..." -ForegroundColor Yellow
python "$PSScriptRoot/render_gui_assets.py"
if ($LASTEXITCODE -ne 0) { Write-Host "[FAIL] GUI Rendering Failed" -ForegroundColor Red; exit 1 }

Write-Host "`n[STEP 2.5] Building Web UI Assets..." -ForegroundColor Yellow
Set-Location "$PSScriptRoot/../web-ui"
if (-not (Test-Path "node_modules")) {
    Write-Host "[INFO] Installing npm dependencies..." -ForegroundColor Gray
    npm install
}
npm run build
if ($LASTEXITCODE -ne 0) { Write-Host "[FAIL] Web UI Build Failed" -ForegroundColor Red; exit 1 }

# 3. Android App Build
Write-Host "`n[STEP 3] Assembling Android Application..." -ForegroundColor Yellow
Set-Location "$PSScriptRoot/.."
./gradlew assembleDebug
if ($LASTEXITCODE -ne 0) { Write-Host "[FAIL] Android Assembly Failed" -ForegroundColor Red; exit 1 }

# 4. Python Sync Module Setup
Write-Host "`n[STEP 4] Synchronizing Python DMX Engine..." -ForegroundColor Yellow
Set-Location "$PSScriptRoot"
if (Test-Path "requirements.txt") {
    python -m pip install -r requirements.txt --quiet
}

Write-Host "`n--- [SUCCESS] Ultima-Grid Build Complete ---" -ForegroundColor Cyan
Write-Host "Output APK: app\build\outputs\apk\debug\app-debug.apk"
Write-Host "Web Server: Port (Dynamic via local.properties)"
Write-Host "Python Sync: scripts/sync_engine.py"