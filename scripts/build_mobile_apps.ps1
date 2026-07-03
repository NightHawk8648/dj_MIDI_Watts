param (
    [switch]$Android,
    [switch]$Ios
)

$ErrorActionPreference = "Stop"

if (-not $Android -and -not $Ios) {
    Write-Host "Please specify -Android and/or -Ios to build." -ForegroundColor Yellow
    exit 1
}

$FlutterDir = Join-Path $PSScriptRoot "..\flutter_ui"

if (-not (Test-Path $FlutterDir)) {
    Write-Host "Error: Flutter UI directory not found at $FlutterDir" -ForegroundColor Red
    exit 1
}

Push-Location $FlutterDir

if ($Android) {
    Write-Host "`n=== Building Android APK/AAB ===" -ForegroundColor Cyan
    Write-Host "Running: flutter build apk --release" -ForegroundColor Gray
    # Assuming flutter is in PATH. If not, this will fail.
    try {
        flutter build apk --release
        Write-Host "Android APK built successfully." -ForegroundColor Green
        
        Write-Host "Running: flutter build appbundle" -ForegroundColor Gray
        flutter build appbundle
        Write-Host "Android App Bundle built successfully." -ForegroundColor Green
    } catch {
        Write-Host "Failed to build Android." -ForegroundColor Red
    }
}

if ($Ios) {
    Write-Host "`n=== Building iOS IPA ===" -ForegroundColor Cyan
    if ($IsWindows) {
        Write-Host "WARNING: iOS builds require macOS/Xcode." -ForegroundColor Yellow
        Write-Host "Bypassing local iOS build. This should be executed on a cloud Mac runner (e.g. GitHub Actions)." -ForegroundColor Yellow
    } elseif ($IsMacOS) {
        Write-Host "Running: flutter build ipa --release --no-codesign" -ForegroundColor Gray
        try {
            flutter build ipa --release --no-codesign
            Write-Host "iOS IPA built successfully." -ForegroundColor Green
        } catch {
            Write-Host "Failed to build iOS." -ForegroundColor Red
        }
    } else {
        Write-Host "iOS build not supported on this OS." -ForegroundColor Red
    }
}

Pop-Location
Write-Host "Build automation complete." -ForegroundColor Green
