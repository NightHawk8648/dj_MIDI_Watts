# DJ MIDI WATTS - Google Cloud Secret Manager Sync & Vault Cache Utility
# This script downloads secrets from GCP Secret Manager and caches them in the secure Windows Vault.

$ErrorActionPreference = "Stop"
Clear-Host

Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "         DJ MIDI WATTS - GOOGLE CLOUD SECURE SECRETS SYNC" -ForegroundColor Cyan -Bold
Write-Host "=====================================================================" -ForegroundColor Cyan

# 1. Enforce TLS 1.2 / 1.3 Secure Handshakes
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 -bor [Net.SecurityProtocolType]::Tls13

# 2. Check for gcloud CLI
$gcloudPath = Get-Command gcloud -ErrorAction SilentlyContinue
if (-not $gcloudPath) {
    Write-Host "[ERROR] Google Cloud SDK (gcloud) is required but not installed." -ForegroundColor Red
    Write-Host "--> Please install Google Cloud SDK: https://cloud.google.com/sdk/docs/install" -ForegroundColor Yellow
    exit 1
}

# 3. Parse GCP Project Configurations
$envCloudFile = Join-Path $PSScriptRoot "..\cloud_env.env"
$userEnvFile = Join-Path $PSScriptRoot "..\user.env"
$projectId = "dj-midi-watts" # Default

if (Test-Path $userEnvFile) {
    $lines = Get-Content $userEnvFile
    foreach ($line in $lines) {
        if ($line -match "^GCP_PROJECT_ID=(.*)") {
            $projectId = $Matches[1].Trim("`"' ")
        }
    }
}
elseif (Test-Path $envCloudFile) {
    $lines = Get-Content $envCloudFile
    foreach ($line in $lines) {
        if ($line -match "^GCP_PROJECT_ID=(.*)") {
            $projectId = $Matches[1].Trim("`"' ")
        }
    }
}
Write-Host "[INFO] Mapped Google Cloud Project: $projectId" -ForegroundColor Gray

# 4. Service Account Authentication Check
$serviceAccountKey = Join-Path $PSScriptRoot "..\secrets\google-play-service.json"
if (Test-Path $serviceAccountKey) {
    $keyContent = Get-Content $serviceAccountKey -Raw
    if ($keyContent -match "PLACEHOLDER") {
        Write-Host "[INFO] Placeholder key detected in google-play-service.json. Relying on active user credentials." -ForegroundColor Gray
    }
    else {
        Write-Host "[SECURITY] Authenticating gcloud utilizing service account credentials..." -ForegroundColor Gray
        try {
            gcloud auth activate-service-account --key-file="$serviceAccountKey" --quiet | Out-Null
            Write-Host "[OK] Authenticated successfully via Service Account." -ForegroundColor Green
        }
        catch {
            Write-Host "[WARNING] Service Account authentication failed. Falling back to default user login..." -ForegroundColor Yellow
        }
    }
}
else {
    Write-Host "[INFO] No local service account key found. Relying on active user credentials." -ForegroundColor Gray
}

# 5. Provisioning Helper Trigger
$provisionScript = Join-Path $PSScriptRoot "provision_credentials.ps1"
if (-not (Test-Path $provisionScript)) {
    Write-Host "[ERROR] Missing credentials provisioning utility at $provisionScript." -ForegroundColor Red
    exit 1
}

# 6. Fetch Secrets from GCP Secret Manager and Sync to Vault
$secretsToSync = @("UG_S1", "UG_S2", "UG_SPOTIFY_CLIENT_ID", "UG_SPOTIFY_CLIENT_SECRET")
$syncedValues = @{}

Write-Host "`nStarting secure fetch from Google Cloud Secret Manager..." -ForegroundColor Gray

foreach ($sec in $secretsToSync) {
    Write-Host "--> Requesting latest version of $sec..." -ForegroundColor Gray
    try {
        # Fetching raw secret payload securely via stdout
        $secretValue = gcloud secrets versions access latest --secret="$sec" --project="$projectId" 2>$null
        if ($secretValue) {
            $secretValue = $secretValue.Trim()
            $syncedValues[$sec] = $secretValue
            Write-Host "[OK] Discovered secret: $sec" -ForegroundColor Green
        }
        else {
            Write-Host "[SKIP] Secret $sec not found or empty." -ForegroundColor Yellow
        }
    }
    catch {
        Write-Host "[SKIP] Could not fetch $sec. Ensure the Secret Manager API is enabled and your account has roles/secretmanager.secretAccessor permissions." -ForegroundColor Yellow
    }
}

# 7. Write to secure vault
if ($syncedValues.Count -gt 0) {
    Write-Host "`nCaching discovered Google Cloud secrets into the Windows Vault..." -ForegroundColor Gray
    
    $s1 = if ($syncedValues.ContainsKey("UG_S1")) { $syncedValues["UG_S1"] } else { "" }
    $s2 = if ($syncedValues.ContainsKey("UG_S2")) { $syncedValues["UG_S2"] } else { "" }
    $spId = if ($syncedValues.ContainsKey("UG_SPOTIFY_CLIENT_ID")) { $syncedValues["UG_SPOTIFY_CLIENT_ID"] } else { "" }
    $spSec = if ($syncedValues.ContainsKey("UG_SPOTIFY_CLIENT_SECRET")) { $syncedValues["UG_SPOTIFY_CLIENT_SECRET"] } else { "" }
    
    # Trigger vault caching
    & $provisionScript -S1_Key $s1 -S2_Key $s2 -Spotify_Client_ID $spId -Spotify_Client_Secret $spSec
    
    Write-Host "`n[SUCCESS] Google Cloud Secret Manager values synced to local Windows Credential Vault." -ForegroundColor Green
    Write-Host "[SECURITY] Secrets are fully encrypted inside the OS vault. Local plaintext configurations can now be safely removed." -ForegroundColor Green
}
else {
    Write-Host "`n[INFO] No secrets were downloaded. To create secrets in Google Cloud Secret Manager, use:" -ForegroundColor Yellow
    Write-Host "   gcloud secrets create <NAME> --replication-policy=\"automatic\"" -ForegroundColor Gray
    Write-Host "   gcloud secrets versions add <NAME> --data-file=\"path/to/key.txt\"" -ForegroundColor Gray
}

Write-Host "=====================================================================" -ForegroundColor Cyan
