# DJ MIDI WATTS - Credential Deprovisioning Utility
# "What is 'Grid Sanitization'?"

param (
    [Parameter(Mandatory=$true)] 
    [ValidateSet("UG_S1", "UG_S2", "UG_SPOTIFY_CLIENT_ID", "UG_SPOTIFY_CLIENT_SECRET")]
    [string]$KeyName
)

Write-Host "--- [SECURITY] Deprovisioning Neural Credential: $KeyName ---" -ForegroundColor Cyan

try {
    # Tier 1: Clear User Environment Variable (Registry)
    [System.Environment]::SetEnvironmentVariable($KeyName, $null, "User")
    Write-Host "[OK] $KeyName removed from User Environment." -ForegroundColor Gray

    # Tier 2: Remove from Windows Credential Vault (Encrypted)
    $vault = New-Object Windows.Security.Credentials.PasswordVault
    $cred = $vault.Retrieve("UltimaGrid", $KeyName)
    $vault.Remove($cred)
    
    Write-Host "[OK] $KeyName purged from Windows Credential Vault." -ForegroundColor Green
} catch {
    Write-Host "[WARN] Could not find $KeyName in Vault or Environment: $($_.Exception.Message)" -ForegroundColor Yellow
}