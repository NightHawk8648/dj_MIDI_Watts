# DJ MIDI WATTS - Credential Provisioning Utility
# "What is 'Grid Sovereignty'?"

param (
    [Parameter(Mandatory=$true)] [string]$S1_Key,
    [Parameter(Mandatory=$true)] [string]$S2_Key,
    [Parameter(Mandatory=$false)] [string]$Spotify_Client_ID = $null,
    [Parameter(Mandatory=$false)] [string]$Spotify_Client_Secret = $null
)

Write-Host "--- [SECURITY] Provisioning Neural Credentials to User Scope ---" -ForegroundColor Cyan

try {
    # Tier 1: User Environment (Registry)
    [System.Environment]::SetEnvironmentVariable("UG_S1", $S1_Key, "User")
    [System.Environment]::SetEnvironmentVariable("GEMINI_API_KEY", $S1_Key, "User")
    [System.Environment]::SetEnvironmentVariable("UG_S2", $S2_Key, "User")
    
    # Tier 2: Windows Credential Vault (Encrypted)
    $vault = New-Object Windows.Security.Credentials.PasswordVault
    $vault.Add((New-Object Windows.Security.Credentials.PasswordCredential("UltimaGrid", "UG_S1", $S1_Key)))
    $vault.Add((New-Object Windows.Security.Credentials.PasswordCredential("UltimaGrid", "UG_S2", $S2_Key)))
    
    if ($Spotify_Client_ID) {
        [System.Environment]::SetEnvironmentVariable("UG_SPOTIFY_CLIENT_ID", $Spotify_Client_ID, "User")
        $vault.Add((New-Object Windows.Security.Credentials.PasswordCredential("UltimaGrid", "UG_SPOTIFY_CLIENT_ID", $Spotify_Client_ID)))
        Write-Host "[OK] Spotify Client ID cached in Vault." -ForegroundColor Gray
    }
    if ($Spotify_Client_Secret) {
        [System.Environment]::SetEnvironmentVariable("UG_SPOTIFY_CLIENT_SECRET", $Spotify_Client_Secret, "User")
        $vault.Add((New-Object Windows.Security.Credentials.PasswordCredential("UltimaGrid", "UG_SPOTIFY_CLIENT_SECRET", $Spotify_Client_Secret)))
        Write-Host "[OK] Spotify Client Secret cached in Vault." -ForegroundColor Gray
    }
    
    Write-Host "[OK] Credentials cached in Environment and Windows Vault." -ForegroundColor Green
    Write-Host "[ACTION] Please restart VS Code / Terminal to synchronize the Grid." -ForegroundColor Yellow
    Write-Host "[WARN] You can now safely delete local.properties." -ForegroundColor Gray
} catch {
    Write-Host "[ERROR] Failed to provision credentials: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}