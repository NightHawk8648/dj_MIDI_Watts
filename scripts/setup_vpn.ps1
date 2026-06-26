# DJ MIDI WATTS - VPN Tunnel Provisioner (Windows)
# Configures Tailscale mesh network for secure remote synchronization

Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "         DJ MIDI WATTS - SECURE MESH VPN PROVISIONING" -ForegroundColor Cyan -Bold
Write-Host "=====================================================================" -ForegroundColor Cyan

# Check if Tailscale is installed
$tailscalePath = Get-Command tailscale -ErrorAction SilentlyContinue

if (-not $tailscalePath) {
    Write-Host "[INFO] Tailscale VPN is not detected on your system PATH." -ForegroundColor Yellow
    Write-Host "       A secure VPN is required to sync your mobile Android app" -ForegroundColor White
    Write-Host "       with this desktop hub remotely when not on the same Wi-Fi." -ForegroundColor White
    
    $response = Read-Host "Would you like to install Tailscale via winget? (y/N)"
    if ($response -match "^y$|^yes$") {
        Write-Host "Installing Tailscale..." -ForegroundColor Gray
        try {
            & winget install Tailscale.Tailscale --silent --accept-package-agreements --accept-source-agreements
            Write-Host "[OK] Tailscale installed. Please restart your terminal/IDE to complete setup." -ForegroundColor Green
        } catch {
            Write-Host "[ERROR] winget installation failed. Please install Tailscale manually from: https://tailscale.com" -ForegroundColor Red
        }
    } else {
        Write-Host "[INFO] Setup skipped. Please set up your mesh VPN tunnel manually." -ForegroundColor Yellow
    }
} else {
    Write-Host "[OK] Tailscale binary detected at: $($tailscalePath.Source)" -ForegroundColor Green
    
    Write-Host "Verifying Tailscale status..." -ForegroundColor Gray
    & tailscale status
    
    Write-Host "`nTo log in and link this device to your mesh network, run:" -ForegroundColor White
    Write-Host "  tailscale up" -ForegroundColor Cyan -Bold
    Write-Host ""
    Write-Host "To obtain the IP of your devices for sync, run:" -ForegroundColor White
    Write-Host "  tailscale ip" -ForegroundColor Cyan
}

Write-Host "=====================================================================" -ForegroundColor Cyan
