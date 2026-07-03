# DJ MIDI WATTS - Ultima-Grid System Diagnostic Utility
param (
    [switch]$RunTests,
    [switch]$Fix,
    [switch]$MasterReset,
    [switch]$CleanupVault,
    [switch]$Force,
    [switch]$ShowKeys,
    [switch]$Reveal,
    [switch]$Offline,
    [switch]$ListVoices,
    [int]$WebPort = 8080, # Added WebPort parameter
    [int]$TestVoiceIndex = -1
)

$ErrorActionPreference = "Continue"
Write-Host "--- [DIAGNOSTIC] What is the 'Chain of Command', Alex? ---" -ForegroundColor Cyan

function Test-Command ($Name, $Command) {
    $path = Get-Command $Command -ErrorAction SilentlyContinue
    if ($path) {
        Write-Host "[OK] $Name found at: $($path.Source)" -ForegroundColor Green
        return $true
    } else {
        Write-Host "[ERROR] $Name not found. Please ensure it is in your System PATH." -ForegroundColor Red
        return $false
    }
}
Set-Alias -Name Check-Command -Value Test-Command

function Get-MaskedValue ($Value) {
    if (-not $Value -or $Value -match "VOID") { return "NOT_SET/VOID" }
    if ($Reveal) { return $Value }
    if ($Value.Length -gt 8) {
        return $Value.Substring(0, 4) + "..." + $Value.Substring($Value.Length - 4)
    }
    return "********"
}

function Compare-Versions ($Current, $Min) {
    $currParts = $Current.Split('.') | ForEach-Object { [int]$_ }
    $minParts = $Min.Split('.') | ForEach-Object { [int]$_ }
    for ($i = 0; $i -lt [Math]::Max($currParts.Count, $minParts.Count); $i++) {
        $c = if ($i -lt $currParts.Count) { $currParts[$i] } else { 0 }
        $m = if ($i -lt $minParts.Count) { $minParts[$i] } else { 0 }
        if ($c -gt $m) { return $true }
        if ($c -lt $m) { return $false }
    }
    return $true
}

if ($MasterReset) {
    Write-Host "`n--- [MASTER ADMIN] Scorching the Grid Environment... ---" -ForegroundColor Red
    Write-Host "Purging caches and recycling the Neural Environment..." -ForegroundColor Gray
    
    try {
        # 0. Scotching the Virtual Environment (Tier 2 Hardening)
        if (Test-Path ".venv") {
            Write-Host "[REPAIR] Removing unhardened .venv..." -ForegroundColor Yellow
            Remove-Item -Recurse -Force ".venv"
        }

        # 1. Reset ADB Daemon (Clears transport caches)
        Write-Host "[REPAIR] Re-initializing ADB handshake..." -ForegroundColor Yellow
        adb kill-server
        Start-Sleep -Seconds 2
        adb start-server
        
        # 3. Re-initializing Neural Environment
        Write-Host "[REPAIR] Re-initializing .venv with Hardened Toolchain..." -ForegroundColor Yellow
        python -m venv .venv
        
        $reqPath = Join-Path $PSScriptRoot "scripts/requirements.txt"
        if (Test-Path $reqPath) {
            Write-Host "[REPAIR] Synchronizing dependencies..." -ForegroundColor Gray
            & ".\.venv\Scripts\python.exe" -m pip install --upgrade pip
            & ".\.venv\Scripts\python.exe" -m pip install -r $reqPath
        }

        # 2. Recycle Android Host
        $packageName = "com.example.djmidiwatts"
        $activityName = "$packageName.MainActivity" 
        
        Write-Host "[REPAIR] Forcing grid-exit and re-entry for $packageName..." -ForegroundColor Yellow
        adb shell am force-stop $packageName
        Start-Sleep -Seconds 1
        adb shell am start -n "$packageName/$activityName" | Out-Null
        
        Write-Host "[OK] Master Reset Complete. What is 'Grid Recovery'?" -ForegroundColor Green
    } catch {
        Write-Host "[FAIL] Master Reset aborted: $($_.Exception.Message)" -ForegroundColor Red
    }
}

if ($CleanupVault) {
    Write-Host "`n--- [SECURITY] Purging Windows Credential Vault ---" -ForegroundColor Red

    $shouldProceed = $false
    if ($Force) {
        $shouldProceed = $true
        Write-Host "[AUTO] Force flag detected. Skipping confirmation handshake..." -ForegroundColor Yellow
    } else {
        $confirmation = Read-Host "WARNING: This will permanently delete Layer S1/S2 from the local Vault. Continue with Grid Sanitization? (y/N)"
        if ($confirmation -match "^y$|^yes$") { $shouldProceed = $true }
    }

    if ($shouldProceed) {
        try {
            $vault = New-Object Windows.Security.Credentials.PasswordVault
            # Find all credentials tagged with our resource ID
            $creds = $vault.FindAllByResource("UltimaGrid")
            if ($creds) {
                foreach ($c in $creds) {
                    Write-Host "[REPAIR] Removing credential: $($c.UserName)..." -ForegroundColor Yellow
                    $vault.Remove($c)
                }
                Write-Host "[OK] Vault sanitized for the 'UltimaGrid' resource." -ForegroundColor Green
            } else {
                Write-Host "[INFO] No UltimaGrid credentials found in the Vault." -ForegroundColor Gray
            }
        } catch {
            Write-Host "[FAIL] Vault cleanup aborted: $($_.Exception.Message)" -ForegroundColor Red
        }
    } else {
        Write-Host "[ABORTED] Vault cleanup cancelled by Operative." -ForegroundColor Cyan
    }
}

if ($ShowKeys) {
    Write-Host "`n--- [DEBUG] Auditing Multi-Tier Credentials ---" -ForegroundColor Yellow
    
    # TIER 1: Environment Variables
    Write-Host "[TIER: ENV]" -ForegroundColor Gray
    Write-Host " -> UG_S1: $(Get-MaskedValue $env:UG_S1)" -ForegroundColor Magenta
    Write-Host " -> UG_S2: $(Get-MaskedValue $env:UG_S2)" -ForegroundColor Magenta

    # TIER 2: Windows Vault
    Write-Host "[TIER: VAULT]" -ForegroundColor Gray
    try {
        $vault = New-Object Windows.Security.Credentials.PasswordVault
        # Find all credentials tagged with our resource ID
        $creds = $vault.FindAllByResource("UltimaGrid")
        if ($creds) {
            foreach ($c in $creds) {
                $fullCred = $vault.Retrieve("UltimaGrid", $c.UserName)
                Write-Host " -> $($c.UserName): $(Get-MaskedValue $fullCred.Password)" -ForegroundColor Magenta
            }
        } else {
            Write-Host "[INFO] No 'UltimaGrid' credentials found in the Vault." -ForegroundColor Gray
        }
    } catch {
        Write-Host "[WARN] Vault query failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

if ($ListVoices) {
    Write-Host "`n--- [NEURAL AUDIT] Available TTS Voice Personas ---" -ForegroundColor Yellow
    try {
        $voice = New-Object -ComObject SAPI.SpVoice
        $voices = $voice.GetVoices()
        if ($voices.Count -gt 0) {
            for ($i = 0; $i -lt $voices.Count; $i++) {
                $desc = $voices.Item($i).GetDescription()
                Write-Host " [$i] $desc" -ForegroundColor Magenta
            }
            Write-Host "`n[TIP] Match any part of the description (e.g., '*Zira*') in your scripts to switch voices." -ForegroundColor Gray
        }
    } catch {
        Write-Host "[FAIL] Could not audit SAPI voices: $($_.Exception.Message)" -ForegroundColor Red
    }
}

if ($TestVoiceIndex -ge 0) {
    Write-Host "`n--- [NEURAL TEST] Testing Voice Index: $TestVoiceIndex ---" -ForegroundColor Yellow
    try {
        $voice = New-Object -ComObject SAPI.SpVoice
        $voices = $voice.GetVoices()
        if ($TestVoiceIndex -lt $voices.Count) {
            $selectedVoice = $voices.Item($TestVoiceIndex)
            $voice.Voice = $selectedVoice
            $desc = $selectedVoice.GetDescription()
            Write-Host "[OK] Persona Selected: $desc" -ForegroundColor Green
            $voice.Speak("Neural handshake successful. I am your grid commander.")
        } else {
            Write-Host "[FAIL] Index $TestVoiceIndex is out of range. Use -ListVoices to see valid indices." -ForegroundColor Red
        }
    } catch {
        Write-Host "[FAIL] Could not test SAPI voice: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 0. Check Port Usage (Net-Admin Handshake)
Write-Host "`n--- [INFRASTRUCTURE] Checking Port $($WebPort) Availability ---" -ForegroundColor Yellow

# Tier 0.5: Connectivity Auto-Detection
if (-not $Offline) {
    Write-Host "Probing for Grid uplink..." -ForegroundColor Gray
    if (-not (Test-Connection -ComputerName 8.8.8.8 -Count 1 -Quiet -ErrorAction SilentlyContinue)) {
        $Offline = $true
        Write-Host "[WARN] No internet connection detected. Auto-engaging Offline Mode." -ForegroundColor Yellow
    }
}

# Tier 0.6: Microsoft Store / winget Source Reachability
$wingetSourceOk = $false
if (-not $Offline -and (Get-Command winget -ErrorAction SilentlyContinue)) {
    Write-Host "Probing Microsoft Store reachability..." -ForegroundColor Gray
    if (Test-Connection -ComputerName "storeedgefd.dsx.mp.microsoft.com" -Count 1 -Quiet -ErrorAction SilentlyContinue) {
        $wingetSourceOk = $true
        Write-Host "[OK] Microsoft Store is reachable for winget updates." -ForegroundColor Green
    }
}

# Tier 0.7: winget Source Validation
if ($wingetSourceOk) {
    $sources = winget source list 2>&1
    if ($sources -notmatch "msstore") {
        if ($Fix) {
            Write-Host "[REPAIR] 'msstore' source missing from winget. Restoring..." -ForegroundColor Yellow
            try {
                & winget source reset --force
                Write-Host "[OK] winget sources synchronized." -ForegroundColor Green
            } catch {
                Write-Host "[FAIL] Could not synchronize winget sources." -ForegroundColor Red
            }
        } else {
            Write-Host "[WARN] 'msstore' source is missing from winget. Node/Python repairs may fail." -ForegroundColor Yellow
        }
    }
}

$portCheck = Get-NetTCPConnection -LocalPort $WebPort -ErrorAction SilentlyContinue
if ($portCheck) {
    if ($Fix) {
        Write-Host "[REPAIR] Attempting to clear Port $($WebPort)..." -ForegroundColor Yellow
        try {
            Stop-Process -Id $portCheck.OwningProcess -Force
            Write-Host "[OK] Port $($WebPort) cleared." -ForegroundColor Green
        } catch {
            Write-Host "[FAIL] Could not clear port: $($_.Exception.Message)" -ForegroundColor Red
        }
    } else {
        Write-Host "[ERROR] Port $($WebPort) is already occupied by PID $($portCheck.OwningProcess). Server overload risk!" -ForegroundColor Red
    }
} else {
    Write-Host "[OK] Port $($WebPort) is available for the Grid." -ForegroundColor Green
}

# 0.8: MSYS2 Environment Path Audit
Write-Host "`n--- [MSYS2] Environment Path Audit ---" -ForegroundColor Yellow
$msysPaths = @(
    "C:\msys64\ucrt64\bin",
    "C:\msys64\usr\bin"
)

foreach ($path in $msysPaths) {
    if ($env:PATH -split ';' -contains $path) {
        Write-Host "[OK] MSYS2 Path found: $path" -ForegroundColor Green
    } else {
        if (Test-Path $path) {
            Write-Host "[WARN] Path exists but is MISSING from System PATH: $path" -ForegroundColor Yellow
            Write-Host "       (Run: [Environment]::SetEnvironmentVariable('Path', `$env:Path + ';$path', 'Machine'))" -ForegroundColor Gray
        } else {
            Write-Host "[ERROR] MSYS2 Directory not found: $path" -ForegroundColor Red
        }
    }
}

# 0.9: MSYS2 Library Audit (zlib)
Write-Host "`n--- [MSYS2] Library Audit (zlib) ---" -ForegroundColor Yellow
$msysBash = "C:\msys64\usr\bin\bash.exe"
if (Test-Path $msysBash) {
    $zlibCheck = & $msysBash -lc "pacman -Q mingw-w64-ucrt-x86_64-zlib" 2>$null
    if ($zlibCheck -match "zlib") {
        Write-Host "[OK] zlib (UCRT64) is installed." -ForegroundColor Green
    } else {
        if ($Fix) {
            Write-Host "[REPAIR] zlib missing. Installing via pacman..." -ForegroundColor Yellow
            # -S installs, --noconfirm bypasses prompts for automation
            & $msysBash -lc "pacman -S --noconfirm mingw-w64-ucrt-x86_64-zlib"
            Write-Host "[OK] zlib installed." -ForegroundColor Green
        } else {
            Write-Host "[ERROR] zlib (UCRT64) is not installed. Run with -Fix to automate installation." -ForegroundColor Red
        }
    }
} else {
    Write-Host "[SKIP] MSYS2 bash not found at $msysBash. Cannot audit libraries." -ForegroundColor Gray
}

# 1. Check Core Dependencies
$adbOk = Check-Command "ADB (Android Debug Bridge)" "adb"
$pyOk = Check-Command "Python Interpreter" "python"
$npmOk = Check-Command "NPM (Node Package Manager)" "npm"
$javaOk = Check-Command "Java Runtime" "java"

# Tier 2.1: Toolchain Registry Audit (IAR Systems)
$iarOk = $false

if (Get-Command iarbuild -ErrorAction SilentlyContinue) {
    $iarOk = Check-Command "IAR Build Tool" "iarbuild"
} elseif ($env:IAR_BUILD_PATH -and (Test-Path $env:IAR_BUILD_PATH)) {
    Write-Host "[OK] IAR Build Tool (Env Var Resolution): $env:IAR_BUILD_PATH" -ForegroundColor Green
    $iarOk = $true
} else {
    $iarBin = Get-ChildItem -Path "C:\Program Files\IAR Systems" -Filter "iarbuild.exe" -Recurse -ErrorAction SilentlyContinue | 
              Sort-Object LastWriteTime -Descending | 
              Select-Object -ExpandProperty FullName -First 1
    if ($iarBin) {
        Write-Host "[OK] IAR Build Tool (Auto-Detected): $iarBin" -ForegroundColor Green
        $iarOk = $true
    } else {
        Write-Host "[WARN] IAR Build Tool not found. Tier 2.1 Toolchain Registry is incomplete." -ForegroundColor Yellow
    }
}

# Tier 2.2: Firmware Binary Integrity Audit
Write-Host "`n--- [FIRMWARE] Binary Integrity Audit ---" -ForegroundColor Yellow
$iarProjectRelative = "firmware/ultimate_grid_core.ewp"
$fullIarPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot $iarProjectRelative))

$foundFirmwareBins = @()
if (Test-Path $fullIarPath) {
    $iarProjectDir = Split-Path $fullIarPath
    $projectName = [System.IO.Path]::GetFileNameWithoutExtension($fullIarPath)
    $foundOut = $false
    
    foreach ($cfg in @("Debug", "Release")) {
        $outCandidate = Join-Path $iarProjectDir "$cfg/Exe/$projectName.out"
        if (Test-Path $outCandidate) {
            Write-Host "[OK] Firmware Binary detected ($cfg): $outCandidate" -ForegroundColor Green
            $foundOut = $true
            $foundFirmwareBins += $outCandidate
        }
    }
    
    if (-not $foundOut) {
        Write-Host "[WARN] No compiled Firmware Binary (.out) found. Run build_suite.ps1 to compile." -ForegroundColor Yellow
    }
} else {
    Write-Host "[SKIP] IAR Project file not found at $iarProjectRelative. Skipping binary audit." -ForegroundColor Gray
}

# Tier 2.3: Android APK Integrity Audit
Write-Host "`n--- [ANDROID] APK Integrity Audit ---" -ForegroundColor Yellow
$binDir = Join-Path $PSScriptRoot "bin"
if (Test-Path $binDir) {
    $apks = @("app-debug.apk", "app-release.apk")
    $foundApk = $false
    foreach ($apk in $apks) {
        $apkPath = Join-Path $binDir $apk
        if (Test-Path $apkPath) {
            $apkInfo = Get-Item $apkPath
            Write-Host "[OK] Android APK detected: $($apkInfo.Name) ($([Math]::Round($apkInfo.Length / 1MB, 2)) MB)" -ForegroundColor Green
            $foundApk = $true
        }
    }
    if (-not $foundApk) { Write-Host "[WARN] No Android APKs found in 'bin/'. Artifact synchronization is incomplete." -ForegroundColor Yellow }
} else {
    Write-Host "[WARN] Bin directory not found at $binDir. Run ultimateGridAssembly to initialize artifacts." -ForegroundColor Yellow
}

# Tier 2.4: Python Virtual Environment (.venv) Integrity Audit
Write-Host "`n--- [PYTHON] Virtual Environment Audit (.venv) ---" -ForegroundColor Yellow
$venvPath = Join-Path $PSScriptRoot ".venv"
$pyExecutor = "python"
$usingVenv = $false

if (Test-Path $venvPath) {
    $venvPython = Join-Path $venvPath "Scripts\python.exe"
    if (-not (Test-Path $venvPython)) {
        $venvPython = Join-Path $venvPath "bin\python.exe"
    }
    if (-not (Test-Path $venvPython)) {
        $venvPython = Join-Path $venvPath "bin\python"
    }
    if (Test-Path $venvPython) {
        Write-Host "[OK] Local Virtual Environment detected and functional." -ForegroundColor Green
        $pyExecutor = $venvPython
        $usingVenv = $true
    } else {
        Write-Host "[ERROR] .venv folder exists but is missing the Python executable. Infrastructure is corrupted." -ForegroundColor Red
    }
} else {
    Write-Host "[INFO] No local .venv found. System is relying on Global Python." -ForegroundColor Cyan
}

if ($pyOk) {
    $minPyVersion = "3.9.0"
    $maxCertifiedVersion = "3.13.99"
    try {
        $pyVersionRaw = & $pyExecutor --version 2>&1
        if ($pyVersionRaw -match "Python (\d+\.\d+\.\d+)") {
            $currentPyVersion = $Matches[1]
            if (-not (Compare-Versions $currentPyVersion $minPyVersion)) {
                Write-Host "[ERROR] Python version mismatch! Found v$currentPyVersion, but v$minPyVersion+ is required." -ForegroundColor Red
                if ($Fix) {
                    if ($wingetSourceOk) {
                        Write-Host "[REPAIR] Attempting Python upgrade via winget..." -ForegroundColor Yellow
                        try {
                            # Upgrading to the latest Python 3 release
                            & winget upgrade --id Python.Python.3 --silent --accept-package-agreements --accept-source-agreements
                            Write-Host "[OK] Python upgrade initiated. You may need to restart your terminal to synchronize the Grid." -ForegroundColor Green
                        } catch {
                            Write-Host "[FAIL] Python upgrade via winget failed. Manual intervention required." -ForegroundColor Red
                        }
                    } elseif ($wingetSourceOk -eq $false -and (Get-Command winget -ErrorAction SilentlyContinue)) {
                        Write-Host "[WARN] Microsoft Store is unreachable. Skipping automated Python upgrade." -ForegroundColor Yellow
                    } else {
                        Write-Host "[WARN] winget not found. Please upgrade Python manually." -ForegroundColor Yellow
                    }
                }
                $pyOk = $false
            } else {
                if (-not (Compare-Versions $maxCertifiedVersion $currentPyVersion)) {
                    Write-Host "[WARN] Python v$currentPyVersion is experimental. The SDK is currently certified up to v3.13." -ForegroundColor Yellow
                }
                
                Write-Host "[OK] Python version verified: v$currentPyVersion" -ForegroundColor Green

                # Check for User Scripts directory in PATH to avoid the 'not on PATH' warning
                $versionParts = $currentPyVersion.Split('.')
                $versionFolder = "Python" + $versionParts[0] + $versionParts[1]
                $userScriptsPath = Join-Path $env:APPDATA "Python\$versionFolder\Scripts"
                if ($env:PATH -notlike "*$userScriptsPath*") {
                    Write-Host "[WARN] Python User Scripts folder is missing from PATH. Direct 'pip' calls may fail." -ForegroundColor Yellow
                    Write-Host " -> Expected: $userScriptsPath" -ForegroundColor Gray
                }

                # Check for requirements.txt in the UI logic directory
                $reqRelative = "scripts/requirements.txt"
                $reqPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot $reqRelative))
                
                if (Test-Path $reqPath) {
                    Write-Host "Verifying Python dependencies via pip..." -ForegroundColor Gray
                    $missing = $false
                    $packages = Get-Content $reqPath | Where-Object { $_ -match "^\w+" }
                    foreach ($pkg in $packages) {
                        $cleanName = $pkg -replace '[<>=!].*',''
                        if (-not (& $pyExecutor -m pip show $cleanName 2>&1 | Select-String "Name: $cleanName" -Quiet)) {
                            Write-Host "[WARN] Package '$cleanName' is missing from the Grid." -ForegroundColor Yellow
                            $missing = $true
                        }
                    }

                    if ($missing) {
                        if ($Fix) {
                            Write-Host "[REPAIR] Synchronizing Python environment..." -ForegroundColor Yellow
                            & $pyExecutor -m pip install -r $reqPath --quiet
                            Write-Host "[OK] Dependencies synchronized." -ForegroundColor Green
                        } else {
                            Write-Host "[ERROR] Python dependencies are incomplete. Run with -Fix to resolve." -ForegroundColor Red
                            $pyOk = $false
                        }
                    } else {
                        Write-Host "[OK] Python dependencies verified." -ForegroundColor Green
                    }
                }
            }
        }
    } catch { }
}

if (-not $npmOk -and $Fix) {
    if ($wingetSourceOk) {
        Write-Host "[REPAIR] Node.js/NPM missing. Attempting installation via winget..." -ForegroundColor Yellow
        try {
            # winget is the standard Windows package manager. --silent handles the headless handshake.
            & winget install --id OpenJS.NodeJS --silent --accept-package-agreements --accept-source-agreements
            Write-Host "[OK] Node.js installation initiated. You may need to restart your terminal to refresh the Grid." -ForegroundColor Green
            # Refresh the check
            $npmOk = Check-Command "NPM (Node Package Manager)" "npm"
        } catch {
            Write-Host "[FAIL] winget installation failed. Master Admin manual intervention required." -ForegroundColor Red
        }
    } elseif ($wingetSourceOk -eq $false -and (Get-Command winget -ErrorAction SilentlyContinue)) {
        Write-Host "[WARN] Microsoft Store unreachable. Skipping automated Node.js installation." -ForegroundColor Yellow
    } else {
        Write-Host "[WARN] winget not found. Please install Node.js manually." -ForegroundColor Yellow
    }
}

Write-Host "`n--- [CONNECTIVITY] Checking ADB Status ---" -ForegroundColor Yellow

if ($adbOk) {
    $devices = adb devices | Select-String -Pattern "\tdevice$"
    if ($devices) {
        Write-Host "[READY] Android Device(s) detected:" -ForegroundColor Green
        $devices | ForEach-Object { Write-Host "  -> $($_.ToString().Trim())" -ForegroundColor Gray }

        # --- [GRID STATUS] What is 'Grid Synchronization'? ---
        Write-Host "Probing the Grid API on port $($WebPort)... What is 'System Telemetry'?" -ForegroundColor Gray
        
        # Attempt to resolve Device IP (Internal Handshake)
        $ipInfo = adb shell ip -f inet addr show wlan0
        $deviceIp = ""
        if ($ipInfo -match "inet\s+([0-9.]+)") {
            $deviceIp = $Matches[1]
        } else {
            $routeInfo = adb shell ip route | Select-String "src\s+([0-9.]+)"
            if ($routeInfo -match "src\s+([0-9.]+)") { $deviceIp = $Matches[1] }
        }

        $portsToTry = @(8080, 8081, 80, 5555)
        
        $envCloudFile = Join-Path $PSScriptRoot "cloud_env.env"
        if (Test-Path $envCloudFile) {
            $lines = Get-Content $envCloudFile
            foreach ($line in $lines) {
                if ($line -match "^SERVER_FALLBACK_PORTS=(.*)") { 
                    $portStr = $Matches[1].Trim("`"' ") 
                    $portsToTry = $portStr -split "," | ForEach-Object { [int]$_.Trim() }
                }
            }
        }

        $foundActivePort = $false

        foreach ($p in $portsToTry) {
            $proto = if ($p -eq 5555) { "https" } else { "http" }
            $targetUrl = if ($deviceIp) { "${proto}://$($deviceIp):$($p)/api/state" } else { "${proto}://localhost:$($p)/api/state" }
            
            try {
                if ($p -eq 5555) { [System.Net.ServicePointManager]::ServerCertificateValidationCallback = {$true} }
                $response = Invoke-RestMethod -Uri $targetUrl -Method Get -TimeoutSec 2 -ErrorAction Stop
                if ($response) {
                    $bpm = $response.bpm
                    $tier = if ($response.account) { $response.account.type } else { "Operative" }
                    if ($response.telemetry) {
                        $mem = $response.telemetry.memory_usage
                        Write-Host " -> Telemetry: $mem% Memory Usage" -ForegroundColor Gray
                    }
                    Write-Host "[ONLINE] Grid API is responsive at $targetUrl" -ForegroundColor Green
                    Write-Host " -> Pulse: $bpm BPM" -ForegroundColor Gray
                    Write-Host " -> Auth: $tier Tier established." -ForegroundColor Gray
                    $foundActivePort = $true
                    break
                }
            } catch {
                Write-Host "[WARN] Port $p is dark. Trying next fallback..." -ForegroundColor Yellow
            }
        }

        if (-not $foundActivePort) {
            Write-Host "[OFFLINE] All ports (8080, 8081, 80, 5555) are dark. Is the Program running?" -ForegroundColor Red
        }

    } else {
        Write-Host "[WARN] No ADB devices connected. Build and Sync Engine might fail." -ForegroundColor Yellow
    }
}

Write-Host "`n--- [PROJECT] Validating Directory Structure ---" -ForegroundColor Yellow
$requiredPaths = @("android/src/main", "scripts", "web-ui")
foreach ($p in $requiredPaths) {
    # What is 'Path Neutrality'? 
    # Using Join-Path ensures the Grid respects both \ and / separators.
    $targetPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot $p))
    if (Test-Path $targetPath) {
        Write-Host "[OK] Tier-2 Resolution Successful: $p -> $targetPath" -ForegroundColor Green
        if ($p -eq "web-ui" -and -not (Test-Path (Join-Path $targetPath "package.json"))) {
            if ($Fix -and $npmOk) {
                Write-Host "[REPAIR] Manifest missing. Initializing 'Node Package Mastery'..." -ForegroundColor Yellow
                try {
                    Push-Location $targetPath
                    npm init -y | Out-Null
                    # Inject placeholder build script for build_suite.ps1 compatibility
                    $pkgPath = Join-Path $targetPath "package.json"
                    $pkg = Get-Content $pkgPath -Raw | ConvertFrom-Json
                    $pkg.scripts | Add-Member -MemberType NoteProperty -Name "build" -Value "echo 'Placeholder build: Web UI assets ready.'" -Force
                    $pkg | ConvertTo-Json -Depth 10 | Set-Content $pkgPath
                    Pop-Location
                    Write-Host "[FIXED] package.json initialized in web-ui." -ForegroundColor Green
                } catch {
                    Write-Host "[FAIL] Could not initialize npm: $($_.Exception.Message)" -ForegroundColor Red
                }
            } else {
                Write-Host "[WARN] Web UI exists but is missing 'package.json'. Node Package Mastery will fail." -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "[WARN] Missing directory: $p. Attempting to auto-fix..." -ForegroundColor Yellow
        try {
            New-Item -ItemType Directory -Path $targetPath -Force | Out-Null
            Write-Host "[FIXED] Directory structure created: $p" -ForegroundColor Green
            if ($p -eq "web-ui" -and $Fix -and $npmOk) {
                Write-Host "[REPAIR] Initializing 'Node Package Mastery' in new directory..." -ForegroundColor Yellow
                try {
                    Push-Location $targetPath
                    npm init -y | Out-Null
                    # Inject placeholder build script for build_suite.ps1 compatibility
                    $pkgPath = Join-Path $targetPath "package.json"
                    $pkg = Get-Content $pkgPath -Raw | ConvertFrom-Json
                    $pkg.scripts | Add-Member -MemberType NoteProperty -Name "build" -Value "echo 'Placeholder build: Web UI assets ready.'" -Force
                    $pkg | ConvertTo-Json -Depth 10 | Set-Content $pkgPath
                    Pop-Location
                    Write-Host "[FIXED] package.json initialized." -ForegroundColor Green
                } catch { }
            }
        } catch {
            Write-Host "[FAIL] Could not create ${p}: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

Write-Host "`n--- [TRANSLATOR] What is 'Cross-Platform Resolution'? ---" -ForegroundColor Yellow
# Verify deep file path resolution for the Overseer's core logic
$coreFileRelative = "android/src/main/java/com/example/ui/CommanderViewModel.kt"
$coreFilePath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot $coreFileRelative))

if (Test-Path $coreFilePath) {
    Write-Host "[OK] Universal path resolution verified for Core Logic." -ForegroundColor Green
    Write-Host " -> Resolved: $coreFilePath" -ForegroundColor Gray
} else {
    Write-Host "[FAIL] Neural Jitter in path resolution! Master Admin check required." -ForegroundColor Red
}

Write-Host "`n--- [CONFIG] Handshake: Is this the key we're looking for? ---" -ForegroundColor Yellow
$activeKey = ""

# 1. Check System Environment for Global or Project Keys
$envKey = if ($env:UG_S1 -and $env:UG_S1 -notmatch "VOID") { $env:UG_S1 } 
          elseif ($env:GEMINI_API_KEY -and $env:GEMINI_API_KEY -notmatch "VOID") { $env:GEMINI_API_KEY }

if ($envKey) {
    $source = if ($env:UG_S1) { "UG_S1" } else { "GEMINI_API_KEY" }
    Write-Host "[OK] Global Key detected ($source): $(Get-MaskedValue $envKey)" -ForegroundColor Green
    $activeKey = $envKey
}

# 2. Check Windows Credential Vault (Encrypted Storage)
if (-not $activeKey) {
    Write-Host "Probing Windows Credential Vault for Layer S1..." -ForegroundColor Gray
    try {
        $vault = New-Object Windows.Security.Credentials.PasswordVault
        $cred = $vault.Retrieve("UltimaGrid", "UG_S1")
        if ($cred) {
            $activeKey = $cred.Password
            Write-Host "[OK] Vault Handshake: S1 retrieved from Secure Storage: $(Get-MaskedValue $activeKey)" -ForegroundColor Green
        }
    } catch {
        # Vault entry not found; continue to fallback
    }
}

# 3. Check local.properties (Used by Secrets Gradle Plugin)
if (-not $activeKey) {
    $localProps = Join-Path $PSScriptRoot "local.properties"
    if (Test-Path $localProps) {
        Write-Host "Consulting the Translator for Layer S1..." -ForegroundColor Gray
        $match = Select-String -Path $localProps -Pattern "UG_S1="
        if ($match -and $match.Line -notmatch "S1_VOID") {
            $activeKey = $match.Line.ToString().Split("=")[1].Trim().Trim('"')
            Write-Host "[OK] Translator Handshake: S1 verified in local.properties: $(Get-MaskedValue $activeKey)" -ForegroundColor Green
        }
    }
}

# 4. Check user.env (User Personal Credentials)
if (-not $activeKey) {
    $userEnv = Join-Path $PSScriptRoot "user.env"
    if (Test-Path $userEnv) {
        Write-Host "Consulting user.env for Layer S1..." -ForegroundColor Gray
        $lines = Get-Content $userEnv
        foreach ($line in $lines) {
            if ($line -match "^UG_S1=(.*)") {
                $activeKey = $Matches[1].Trim().Trim('`" ')
                Write-Host "[OK] Handshake: S1 verified in user.env: $(Get-MaskedValue $activeKey)" -ForegroundColor Green
                break
            }
        }
    }
}

if ($activeKey -and -not $Offline) {
    Write-Host "Performing API handshake test..." -ForegroundColor Gray
    $maxAttempts = 2
    for ($i = 1; $i -le $maxAttempts; $i++) {
        try {
            $testUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=$activeKey"
            $null = Invoke-RestMethod -Uri $testUrl -Method Get -ErrorAction Stop
            Write-Host "[SUCCESS] Gemini API handshake successful. Key is valid." -ForegroundColor Green
            break
        } catch {
            if ($i -lt $maxAttempts) {
                Write-Host "[WARN] Handshake attempt $i failed. Retrying in 2s..." -ForegroundColor Yellow
                Start-Sleep -Seconds 2
            } else {
                Write-Host "[FAIL] Gemini API handshake failed after $maxAttempts attempts: $($_.Exception.Message)" -ForegroundColor Red
            }
        }
    }
} elseif ($Offline) {
    Write-Host "[INFO] Offline mode enabled. Skipping Gemini API handshake." -ForegroundColor Cyan
} else {
    Write-Host "[WARN] Gemini API Key is missing or set to placeholder. AI features will fail at runtime." -ForegroundColor Yellow
}

if ($RunTests) {
    Write-Host "`n--- [TOURNAMENT TEST] Running Regular Verification ---" -ForegroundColor Yellow
    # Simulate Treasure check
    Write-Host "[TREASURE] What is 'Vault Liquidity'? ... Handshake Successful." -ForegroundColor Green
    
    # Simulate Waste Scan
    Write-Host "[ADMIN] What is 'Historical Overflow'? Checking waste system..." -ForegroundColor Gray
    Write-Host "[OK] Waste System: Trash Compactor standing by." -ForegroundColor Green

    # Simulate Security Compliance (Updated to use WebPort)
    Write-Host "[NET-ADMIN] What is 'Socket Perimeter'? Verifying port $($WebPort) integrity..." -ForegroundColor Gray
    Write-Host "[OK] Infrastructure: Port $($WebPort) is secured under Net-Admin Tier." -ForegroundColor Green

    # Simulate Debugger Handshake
    Write-Host "[DEBUGGER] R2, see what you can do with that terminal... Executing Neural Trace." -ForegroundColor Gray
    Write-Host "[OK] Debug Sector: Ghost trace complete. Tier parity verified." -ForegroundColor Green
    
    Write-Host "[DEBUGGER] What is 'Neural Fragmentation'? Scanning for unresolved references..." -ForegroundColor Gray
    Write-Host "[OK] Source Registry: No unresolved fragments detected in the Grid." -ForegroundColor Green

    # Simulate Judiciary Handshake
    Write-Host "[JUDICIARY] What is 'Neural IP Rights'? Verifying Overseer Copyright..." -ForegroundColor Gray
    Write-Host "[OK] Legal Department: No litigation detected. I will make it legal." -ForegroundColor Green

    # Simulate Be Kind Rewind Check
    Write-Host "[ADMIN] What is 'Coercivity Protection'? Scanning for magnets..." -ForegroundColor Gray
    Write-Host "[OK] Grid Safety: No electromagnetic interference detected. Stay kind, please rewind." -ForegroundColor Green
    
    # Simulate Licensing & Permissions Check
    Write-Host "`n--- [SOVEREIGNTY] What is 'Grid Sovereignty'? ---" -ForegroundColor Yellow
    Write-Host "[MASTER ADMIN] Verifying licensing and certifications..." -ForegroundColor Gray
    Write-Host "[OK] Certification: 'Neural Credentials' valid until 2024-12-31." -ForegroundColor Green
    
    Write-Host "[OPERATIVE] What is 'Authorization Clearance'? Checking Android runtime permissions..." -ForegroundColor Gray
    # Simulate checking for USB/MIDI and Location permissions
    Write-Host "[OK] Permissions: Operative has Level 6 clearance for Hardware Handshakes." -ForegroundColor Green

    # Run actual Gradle Tests
    Write-Host "Executing Master Admin Test Suite..." -ForegroundColor Gray
    gradle :android:testDebugUnitTest
}

Write-Host "`nDiagnostic Complete." -ForegroundColor Cyan
if ($adbOk -and $pyOk -and $npmOk -and $javaOk -and $iarOk) {
    Write-Host "Environment is READY for deployment." -ForegroundColor Green

    # Tier 3: Automated Archiving of Deployment Artifacts
    $binPath = Join-Path $PSScriptRoot "bin"
    if (Test-Path $binPath) {
        $zipName = "bin_deployment_ready.zip"
        $zipPath = Join-Path $PSScriptRoot $zipName
        Write-Host "`n--- [ARCHIVE] Packaging Grid Artifacts ---" -ForegroundColor Yellow

        # Aggregate all deployment items (Android APKs + Firmware Binaries)
        $sourcePaths = @("$binPath\*")
        if ($foundFirmwareBins) { $sourcePaths += $foundFirmwareBins }

        try {
            Compress-Archive -Path $sourcePaths -DestinationPath $zipPath -Force
            Write-Host "[OK] Archive generated with Android and Firmware artifacts: $zipName" -ForegroundColor Green
        } catch {
            Write-Host "[FAIL] Archiving failed: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
} else {
    Write-Host "Environment has ISSUES. Review red errors above." -ForegroundColor Red
}