$ErrorActionPreference = "Stop"
Clear-Host

Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "         DJ MIDI WATTS - CLOUD RUN DEPLOYMENT SCRIPT" -ForegroundColor Cyan -Bold
Write-Host "=====================================================================" -ForegroundColor Cyan

# Parse GCP Project Configurations
$envCloudFile = Join-Path $PSScriptRoot "..\cloud_env.env"
$userEnvFile = Join-Path $PSScriptRoot "..\user.env"
$projectId = "dj-midi-watts" # Default
$region = "us-central1"

if (Test-Path $userEnvFile) {
    $lines = Get-Content $userEnvFile
    foreach ($line in $lines) {
        if ($line -match "^GCP_PROJECT_ID=(.*)") { $projectId = $Matches[1].Trim("`"' ") }
        if ($line -match "^GCP_REGION=(.*)") { $region = $Matches[1].Trim("`"' ") }
    }
}
elseif (Test-Path $envCloudFile) {
    $lines = Get-Content $envCloudFile
    foreach ($line in $lines) {
        if ($line -match "^GCP_PROJECT_ID=(.*)") { $projectId = $Matches[1].Trim("`"' ") }
        if ($line -match "^GCP_REGION=(.*)") { $region = $Matches[1].Trim("`"' ") }
    }
}

Write-Host "[INFO] Deploying to Project: $projectId in Region: $region" -ForegroundColor Gray

# Deploy to Cloud Run
$sourcePath = Join-Path $PSScriptRoot ".."

$deployCommand = "gcloud run deploy dj-midi-watts-api --source `"$sourcePath`" --project `"$projectId`" --region `"$region`" --allow-unauthenticated "
$deployCommand += "--set-secrets=UG_S1=UG_S1:latest,UG_S2=UG_S2:latest,UG_S3_KEYSTORE_PASS=UG_S3_KEYSTORE_PASS:latest,PLAY_API_KEY=PLAY_API_KEY:latest "
$deployCommand += "--set-secrets=/secrets/google-play-service.json=GOOGLE_PLAY_SERVICE_JSON:latest "
$deployCommand += "--set-env-vars=GOOGLE_APPLICATION_CREDENTIALS=/secrets/google-play-service.json"

Write-Host "[DEPLOY] Executing Cloud Run Deployment..." -ForegroundColor Yellow
Invoke-Expression $deployCommand

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[SUCCESS] Deployment completed successfully!" -ForegroundColor Green
} else {
    Write-Host "`n[ERROR] Deployment failed. Please check the logs above." -ForegroundColor Red
}

Write-Host "=====================================================================" -ForegroundColor Cyan
