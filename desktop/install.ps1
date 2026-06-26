# DJ MIDI WATTS - Desktop Clean Installation Installer
# This script prepares dependencies, terminates background port locks, and runs initial hardware syncs.

$ErrorActionPreference = "Continue"
Clear-Host

Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "         DJ MIDI WATTS - DESKTOP CLEAN INSTALLATION PROCESS" -ForegroundColor Cyan -Bold
Write-Host "=====================================================================" -ForegroundColor Cyan

# 0. Security Compliance Prompt (Cloud & VPN Enforcement Audit)
Write-Host "=====================================================================" -ForegroundColor Yellow
Write-Host "  SECURITY COMPLIANCE AUDIT: PERSONAL CLOUD & VPN TUNNEL" -ForegroundColor Yellow -Bold
Write-Host "=====================================================================" -ForegroundColor Yellow
Write-Host "If you choose NOT to deploy and run your own secure, decentralized" -ForegroundColor White
Write-Host "Personal Cloud (Nextcloud) and choose NOT to use an encrypted mesh VPN" -ForegroundColor White
Write-Host "(Tailscale) to bridge remote connections, your personal API keys," -ForegroundColor White
Write-Host "credentials, and local device caches will be exposed without proper" -ForegroundColor White
Write-Host "tunneling. YOUR SENSITIVE INFORMATION MIGHT BE AT RISK." -ForegroundColor Red -Bold
Write-Host "=====================================================================" -ForegroundColor Yellow
$response = Read-Host "Do you acknowledge this security risk and wish to proceed? (y/N)"
if ($response -notmatch "^y$|^yes$") {
    Write-Host "[SECURITY] Installation aborted to allow securing your configuration." -ForegroundColor Red
    exit 1
}
Write-Host ""

# 1. Clean up stale server processes to free up Port 8000
Write-Host "Clearing stale PHP server threads..." -ForegroundColor Gray
Stop-Process -Name "php" -Force -ErrorAction SilentlyContinue
$port8000 = Get-NetTCPConnection -LocalPort 8000 -ErrorAction SilentlyContinue
if ($port8000) {
    Write-Host "[WARNING] Port 8000 is still bound. Attempting process cleanup..." -ForegroundColor Yellow
    foreach ($conn in $port8000) {
        Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
    }
}
Write-Host "[OK] Clean environment achieved." -ForegroundColor Green

# 2. Re-install Node launcher dependencies
Write-Host "`nInitializing Node.js package installation..." -ForegroundColor Gray
$desktopDir = "$PSScriptRoot"

# Remove existing node_modules if present to ensure a clean install
$nodeModules = Join-Path $desktopDir "node_modules"
if (Test-Path $nodeModules) {
    Write-Host "Removing existing desktop/node_modules cache..." -ForegroundColor Gray
    Remove-Item -Recurse -Force $nodeModules -ErrorAction SilentlyContinue
}

# Run npm install inside desktop folder
Push-Location $desktopDir
try {
    Write-Host "Running npm install inside /desktop folder..." -ForegroundColor Cyan
    npm install --no-audit --no-fund
    Write-Host "[OK] Desktop server packages installed successfully." -ForegroundColor Green
} catch {
    Write-Host "[ERROR] npm install failed: $($_.Exception.Message)" -ForegroundColor Red
}
Pop-Location

# 3. Synchronize hardware profiles using preboot discovery
Write-Host "`nRunning Preboot Device Discovery..." -ForegroundColor Gray
$prebootScript = Join-Path $PSScriptRoot "..\drivers\preboot_device_discovery.ps1"
if (Test-Path $prebootScript) {
    & $prebootScript
} else {
    Write-Host "[WARNING] Preboot discovery script not found at $prebootScript." -ForegroundColor Yellow
}

# 4. Chrome Extension Instructions
Write-Host "`n=====================================================================" -ForegroundColor Cyan
Write-Host "         CHROME EXTENSION UNPACKED LOADING DETAILS" -ForegroundColor Cyan
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "When you start the desktop program via: npm start, it will automatically" -ForegroundColor Gray
Write-Host "launch Google Chrome with the DJ-MIDI-WATTS extension loaded unpacked." -ForegroundColor Gray
Write-Host ""
Write-Host "To verify or load it manually:" -ForegroundColor Gray
Write-Host "  1. Open Chrome and navigate to: chrome://extensions/" -ForegroundColor Gray
Write-Host "  2. Toggle 'Developer mode' (top right corner) to ON." -ForegroundColor Gray
Write-Host "  3. Click 'Load unpacked' (top left corner)." -ForegroundColor Gray
Write-Host "  4. Select the directory:" -ForegroundColor Gray
Write-Host "     c:\Users\Night\dj_MIDI_Watts\dj-midi-watts-extension" -ForegroundColor Cyan
Write-Host ""
Write-Host "The extension will synchronize dashboard telemetry in the background." -ForegroundColor Green
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "Clean install completed! Run 'npm start' inside /desktop to launch." -ForegroundColor Green
Write-Host "=====================================================================" -ForegroundColor Cyan
