# DJ-MIDI-WATTS - Desktop App Installer Builder
# This script bundles the built files, assets, and scripts into a Windows deployment package.

$ErrorActionPreference = "Stop"
$ProjectRoot = Resolve-Path "$PSScriptRoot\..\.."
$DesktopDir = Join-Path $ProjectRoot "desktop"
$DistDir = Join-Path $DesktopDir "dist"
$ZipPath = Join-Path $DistDir "dj-midi-watts-desktop-installer.zip"

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "   BUILDING DESKTOP DEPLOYMENT PACKAGES      " -ForegroundColor Cyan -Bold
Write-Host "=============================================" -ForegroundColor Cyan

# 1. Ensure dist folder exists
if (-not (Test-Path $DistDir)) {
    New-Item -ItemType Directory -Path $DistDir -Force | Out-Null
}

# 2. Package everything into a zip installer
Write-Host "Aggregating desktop launcher, scripts, and server code..." -ForegroundColor Gray
$excludeList = @("node_modules", "dist", "build", ".*")
$filesToPackage = Get-ChildItem -Path $DesktopDir -Exclude $excludeList

Write-Host "Creating deployment archive at: $ZipPath" -ForegroundColor Gray
Compress-Archive -Path $filesToPackage.FullName -DestinationPath $ZipPath -Force

Write-Host "[SUCCESS] Windows Desktop zip package completed: $ZipPath" -ForegroundColor Green
