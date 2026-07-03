param (
    [string]$AppxManifestPath = "$PSScriptRoot\..\desktop\AppxManifest.xml",
    [string]$SourceDir = "$PSScriptRoot\..\desktop\dist",
    [string]$OutputDir = "$PSScriptRoot\..\build",
    [string]$OutputMsix = "$PSScriptRoot\..\build\DJ_MIDI_Watts.msix",
    [string]$CertPath = "$PSScriptRoot\..\windows-cert.pfx",
    [string]$CertPassword = ""
)

$ErrorActionPreference = "Stop"

Write-Host "`n=== DJ MIDI Watts MSIX Packaging Pipeline ===" -ForegroundColor Cyan

# Ensure build directory exists
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

# Ensure Windows SDK Tools (MakeAppx and SignTool) are available
$WindowsKitsDir = "C:\Program Files (x86)\Windows Kits\10\bin"
$LatestKit = Get-ChildItem -Path $WindowsKitsDir -Directory | Sort-Object Name -Descending | Select-Object -First 1
$Arch = if ([Environment]::Is64BitOperatingSystem) { "x64" } else { "x86" }
$MakeAppxPath = Join-Path $LatestKit.FullName "$Arch\makeappx.exe"
$SignToolPath = Join-Path $LatestKit.FullName "$Arch\signtool.exe"

if (-not (Test-Path $MakeAppxPath)) {
    Write-Host "[ERROR] MakeAppx.exe not found. Is the Windows SDK installed?" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $AppxManifestPath)) {
    Write-Host "[ERROR] AppxManifest.xml not found at $AppxManifestPath" -ForegroundColor Red
    exit 1
}

# 1. Package the MSIX
Write-Host "`n[1/2] Packaging MSIX using MakeAppx..." -ForegroundColor Yellow
$MakeAppxCmd = "& `"$MakeAppxPath`" pack /d `"$SourceDir`" /p `"$OutputMsix`" /o"
Write-Host "Running: $MakeAppxCmd" -ForegroundColor Gray
Invoke-Expression $MakeAppxCmd

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] MakeAppx failed." -ForegroundColor Red
    exit 1
}
Write-Host "[SUCCESS] MSIX Packaged at $OutputMsix" -ForegroundColor Green

# 2. Sign the MSIX
Write-Host "`n[2/2] Signing MSIX using SignTool..." -ForegroundColor Yellow
if (Test-Path $CertPath) {
    $SignToolCmd = "& `"$SignToolPath`" sign /a /v /fd SHA256 /f `"$CertPath`" `"$OutputMsix`""
    if ($CertPassword) {
        $SignToolCmd = "& `"$SignToolPath`" sign /a /v /fd SHA256 /f `"$CertPath`" /p `"$CertPassword`" `"$OutputMsix`""
    }
    Write-Host "Running: $SignToolCmd" -ForegroundColor Gray
    Invoke-Expression $SignToolCmd

    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] SignTool failed." -ForegroundColor Red
        exit 1
    }
    Write-Host "[SUCCESS] MSIX successfully signed!" -ForegroundColor Green
} else {
    Write-Host "[WARNING] Certificate not found at $CertPath. Package is unsigned and may not install." -ForegroundColor Magenta
}

Write-Host "`n=== Pipeline Complete ===" -ForegroundColor Cyan
