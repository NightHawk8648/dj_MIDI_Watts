$ProjectRoot = Resolve-Path "$PSScriptRoot\.."
$SrcExe = Join-Path $ProjectRoot "desktop\bin\hardware_manager.exe"
$DestBin = Join-Path $ProjectRoot "bin\hardware_manager.exe"

if (-not (Test-Path "$ProjectRoot\bin")) {
    New-Item -ItemType Directory -Path "$ProjectRoot\bin" -Force | Out-Null
}

if (Test-Path $SrcExe) {
    Copy-Item -Path $SrcExe -Destination $DestBin -Force
    Write-Host "[OK] Copied hardware_manager.exe to root bin/ directory." -ForegroundColor Green
} else {
    Write-Warning "Source executable not found at $SrcExe. Please compile first."
}
