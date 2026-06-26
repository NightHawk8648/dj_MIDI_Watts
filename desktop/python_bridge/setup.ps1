# Powershell Setup Orchestrator for Windows Environments

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Push-Location $ScriptDir

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host "     BOOTSTRAPPING PYTHON BRIDGE ENVIRONMENT (WINDOWS)    " -ForegroundColor Cyan -Bold
Write-Host "=========================================================" -ForegroundColor Cyan

# Check for CMake
$hasCMake = Get-Command cmake -ErrorAction SilentlyContinue

if ($hasCMake) {
    Write-Host "[BUILD] Found CMake. Configuring native diagnostics..." -ForegroundColor Gray
    
    if (-not (Test-Path "build")) {
        New-Item -ItemType Directory -Path "build" | Out-Null
    }
    
    Push-Location "build"
    try {
        # Configure and build using CMake
        & cmake .. -DCMAKE_BUILD_TYPE=Release
        & cmake --build . --config Release
        
        Pop-Location
        
        $binPath = "build/Release/python_bridge_audit.exe"
        if (-not (Test-Path $binPath)) {
            $binPath = "build/python_bridge_audit.exe"
        }
        
        if (Test-Path $binPath) {
            Write-Host "[EXEC] Running native environment diagnostics..." -ForegroundColor Green
            & $binPath
            Pop-Location
            exit $LASTEXITCODE
        } else {
            Write-Host "[WARN] Native binary not found. Falling back to direct script execution." -ForegroundColor Yellow
        }
    } catch {
        Write-Host "[WARN] CMake build failed. Falling back to direct script execution." -ForegroundColor Yellow
        Pop-Location
    }
} else {
    Write-Host "[INFO] CMake not found. Bootstrapping directly via script fallback..." -ForegroundColor Gray
}

# Fallback: execute setup_env.py directly
$pythonCmd = "python"
if (-not (Get-Command $pythonCmd -ErrorAction SilentlyContinue)) {
    $pythonCmd = "python3"
}

if (Get-Command $pythonCmd -ErrorAction SilentlyContinue) {
    Write-Host "[EXEC] Executing Python bootstrap setup script..." -ForegroundColor Green
    & $pythonCmd "scripts/setup_env.py"
} else {
    Write-Host "[ERROR] Python interpreter not found on the system PATH." -ForegroundColor Red
    Write-Host "        Please install Python 3.14 or run 'uv python install' first." -ForegroundColor Yellow
}

Pop-Location
