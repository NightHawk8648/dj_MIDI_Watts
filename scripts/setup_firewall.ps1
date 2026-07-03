# ============================================================================
# DJ MIDI WATTS - SECURE FIREWALL PROVISIONING
# ============================================================================
# This script configures Windows Defender Firewall to allow inbound 
# traffic on necessary Grid ports, strictly locked down to your Local Subnet.
# Run this as Administrator!

$RuleNamePrefix = "DJ_MIDI_WATTS"
$Ports = @(8080, 8081, 80, 5555)

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host " DJ MIDI WATTS - SECURE FIREWALL PROVISIONING" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

# Check for Administrator privileges
$currentPrincipal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $currentPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "[ERROR] Administrator privileges are required to configure the firewall." -ForegroundColor Red
    Write-Host "Please right-click this script and 'Run as Administrator', or run from an elevated PowerShell." -ForegroundColor Yellow
    exit
}

# User Prompt & Notification
Write-Host "`n[SECURITY NOTIFICATION]" -ForegroundColor Yellow
Write-Host "In order for the DJ MIDI Watts Grid and local Hub to function securely and properly,"
Write-Host "exceptions must be added to the Windows Defender Firewall."
Write-Host "We are applying a STRICT LOCKDOWN policy:"
Write-Host "  - Only devices on your immediate local network (LocalSubnet) can connect."
Write-Host "  - Exceptions will ONLY apply to 'Private' network profiles (e.g. Home)."
Write-Host "  - Public Wi-Fi connections will remain completely blocked to prevent external access.`n"

$confirmation = Read-Host "Do you consent to adding these secure firewall exceptions? (Type 'Y' or 'Yes' to proceed)"
if ($confirmation -notmatch "^(y|yes|Y|Yes|YES)$") {
    Write-Host "`n[ABORTED] Firewall provisioning cancelled by user. The application may not communicate correctly across devices." -ForegroundColor Red
    exit
}

Write-Host "`n[PROCEEDING] Applying secure firewall rules..." -ForegroundColor Cyan

foreach ($port in $Ports) {
    $inboundName = "${RuleNamePrefix}_Inbound_TCP_${port}"
    
    # Remove existing permissive rules if they exist
    $existing = Get-NetFirewallRule -DisplayName $inboundName -ErrorAction SilentlyContinue
    if ($existing) {
        Write-Host "  - [CLEANUP] Removing old permissive rule: $inboundName" -ForegroundColor Gray
        Remove-NetFirewallRule -DisplayName $inboundName
    }

    Write-Host "  - [SECURE] Adding strictly scoped Inbound TCP Exception for Port $port" -ForegroundColor Green
    
    # Create the rule locked to Private profiles and LocalSubnet scope
    New-NetFirewallRule -DisplayName $inboundName `
                        -Direction Inbound `
                        -LocalPort $port `
                        -Protocol TCP `
                        -Action Allow `
                        -Profile Private `
                        -RemoteAddress LocalSubnet | Out-Null
}

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "[SUCCESS] Firewall exceptions securely applied." -ForegroundColor Green
Write-Host "Grid ports (8080, 8081, 80, 5555) are open to trusted local devices." -ForegroundColor Green
Write-Host "=======================================================" -ForegroundColor Cyan
