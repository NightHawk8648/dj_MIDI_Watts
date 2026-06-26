# DJ-MIDI-WATTS - Ultima-Grid Master Build Suite
# Tier 0: Deployment Overlord logic

param (
    [string]$IarConfig,
    [string]$IarPath,
    [string]$IarProject = "firmware/ultimate_grid_core.ewp", 
    [string]$BuildType = "Debug",
    [string]$KeyStorePath = "keystore/release.jks",
    [string]$KeyAlias = "ultima-grid",
    [switch]$SkipDiagnostics,
    [switch]$Reveal,
    [switch]$Flash,
    [switch]$ForceBuild
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot | Split-Path -Parent

# --- Neural Alignment: Synchronize IAR Config with Android BuildType ---
if (-not $PSBoundParameters.ContainsKey('IarConfig')) {
    $IarConfig = $BuildType
    Write-Host "[CONFIG] Aligning IAR Toolchain to '$IarConfig' to match Android BuildType." -ForegroundColor Gray
}

Write-Host "--- [DEPLOYMENT] Initiating Grid Build Sequence ---" -ForegroundColor Cyan

# 1. Execute Neural Handshake (Diagnostics)
if (-not $SkipDiagnostics) {
    Write-Host "`n[STEP 1/5] Running System Diagnostics..." -ForegroundColor Yellow
    & "$PSScriptRoot\..\foo.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Diagnostics failed. Deployment aborted to prevent Grid instability."
        exit $LASTEXITCODE
    }
}

# 2. Build Web UI Assets
Write-Host "`n[STEP 2/5] Compiling Web UI Mastery..." -ForegroundColor Yellow
Push-Location "$ProjectRoot\web-ui"
try {
    npm install
    npm run build
} finally {
    Pop-Location
}

# 3. Build IAR Toolchain Project (The IAR Move)
Write-Host "`n[STEP 3/5] Executing IAR Toolchain Build & Flash..." -ForegroundColor Yellow

$fullProjectPath = Join-Path $ProjectRoot $IarProject
if (Test-Path $fullProjectPath) {
    if ($IarPath -and (Test-Path $IarPath)) {
        $iarBin = $IarPath
    } elseif (Get-Command iarbuild -ErrorAction SilentlyContinue) {
        $iarBin = "iarbuild"
    } elseif ($env:IAR_BUILD_PATH -and (Test-Path $env:IAR_BUILD_PATH)) {
        $iarBin = $env:IAR_BUILD_PATH
    } else {
        # Auto-detection fallback: Find the latest iarbuild.exe in Program Files
        $iarBin = Get-ChildItem -Path "C:\Program Files\IAR Systems" -Filter "iarbuild.exe" -Recurse -ErrorAction SilentlyContinue | 
                  Sort-Object LastWriteTime -Descending | 
                  Select-Object -ExpandProperty FullName -First 1
    }

    if (-not $iarBin) {
        Write-Error "IAR Build Tool (iarbuild.exe) not found. Tier 2.1 Toolchain Registry is incomplete."
        exit 1
    }

    $iarProjectDir = Split-Path $fullProjectPath
    $projectName = [System.IO.Path]::GetFileNameWithoutExtension($fullProjectPath)
    $outputFile = Join-Path $iarProjectDir "$IarConfig/Exe/$projectName.out"
    
    $needsBuild = $true
    if (-not $ForceBuild -and (Test-Path $outputFile)) {
        # Scan for the latest modification in source files
        $sourceFiles = Get-ChildItem -Path $iarProjectDir -Recurse -File | 
                       Where-Object { $_.Extension -match "\.(c|cpp|h|hpp|s|ewp)$" }
        $latestSourceTime = ($sourceFiles | Measure-Object -Property LastWriteTime -Maximum).Maximum
        $outputTime = (Get-Item $outputFile).LastWriteTime
        
        if ($outputTime -ge $latestSourceTime) {
            Write-Host "[SKIP] Firmware is UP-TO-DATE. (Use -ForceBuild to override)" -ForegroundColor Cyan
            $needsBuild = $false
        }
    }

    if ($needsBuild) {
        Write-Host "Building IAR Project: $IarProject ($IarConfig)" -ForegroundColor Gray
        & $iarBin $fullProjectPath -build $IarConfig
        if ($LASTEXITCODE -ne 0) {
            Write-Error "IAR Build failed."
            exit $LASTEXITCODE
        }
        Write-Host "[OK] Firmware compiled." -ForegroundColor Green
    }

    # --- Automated Flashing Sequence ---
    if ($Flash) {
        Write-Host "Initiating Firmware Flash to Hardware..." -ForegroundColor Cyan
        & $iarBin $fullProjectPath -download $IarConfig
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Firmware flash failed. Hardware handshake aborted."
            exit $LASTEXITCODE
        }
        Write-Host "[OK] Hardware flashed and ready." -ForegroundColor Green
    }
} else {
    Write-Host "[WARN] IAR Project file not found at $fullProjectPath. Skipping firmware build." -ForegroundColor Yellow
}

# 4. Assemble Android Grid Program
Write-Host "`n[STEP 4/5] Assembling Android Grid Program ($BuildType)..." -ForegroundColor Yellow
Push-Location $ProjectRoot
try {
    $gradleCmd = if ($IsWindows) { ".\gradlew.bat" } else { "./gradlew" }
    & $gradleCmd "assemble$BuildType"
} finally {
    Pop-Location
}

# 5. Secure Signing Handshake (Release Only)
if ($BuildType -eq "Release") {
    Write-Host "`n[STEP 5/5] Executing Vault-Backed Signing..." -ForegroundColor Yellow
    
    try {
        # Retrieve KeyStore Password from Windows Vault (Tier 5 logic)
        $vault = New-Object Windows.Security.Credentials.PasswordVault
        $cred = $vault.Retrieve("UltimaGrid", "UG_S3_KEYSTORE_PASS")
        $ksPassword = $cred.Password
        
        Write-Host "[OK] Vault Handshake: Signing credentials retrieved." -ForegroundColor Green
        
        # Locate apksigner in Android SDK
        $sdkPath = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
        $apkSigner = Get-ChildItem -Path "$sdkPath\build-tools" -Filter "apksigner.bat" -Recurse | Select-Object -First 1
        
        if (-not $apkSigner) { throw "apksigner.bat not found in Android SDK build-tools." }
        
        # Find the unsigned APK
        $unsignedApk = Get-ChildItem -Path "$ProjectRoot\android\build\outputs\apk\release\*-unsigned.apk" | Select-Object -First 1
        if (-not $unsignedApk) { $unsignedApk = Get-ChildItem -Path "$ProjectRoot\android\build\outputs\apk\release\*.apk" | Select-Object -First 1 }
        
        Write-Host "Signing APK: $($unsignedApk.Name)" -ForegroundColor Gray
        
        # Execute Signing
        & $apkSigner.FullName sign --ks $KeyStorePath --ks-key-alias $KeyAlias --ks-pass "pass:$ksPassword" $unsignedApk.FullName
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[OK] Release APK signed successfully." -ForegroundColor Green
        } else {
            throw "Signing failed with exit code $LASTEXITCODE"
        }
    } catch {
        Write-Host "[CRITICAL] Signing failed: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n--- [SUCCESS] Grid Deployment Ready. 'Stay on target...' ---" -ForegroundColor Green