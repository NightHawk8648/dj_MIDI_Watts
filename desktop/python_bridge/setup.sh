#!/bin/bash
# Shell Setup Orchestrator for Unix/macOS/Linux/Debian/Ubuntu

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo -e "\033[36m=========================================================\033[0m"
echo -e "\033[36m;1m     BOOTSTRAPPING PYTHON BRIDGE ENVIRONMENT (UNIX)    \033[0m"
echo -e "\033[36m=========================================================\033[0m"

# Check for CMake
if command -v cmake >/dev/null 2>&1; then
    echo "[BUILD] Found CMake. Configuring native diagnostics..."
    mkdir -p build
    cd build
    
    cmake .. -DCMAKE_BUILD_TYPE=Release
    make
    
    if [ -f "./python_bridge_audit" ]; then
        echo -e "\033[32m[EXEC] Running native environment diagnostics...\033[0m"
        ./python_bridge_audit
        EXIT_CODE=$?
        cd "$SCRIPT_DIR"
        exit $EXIT_CODE
    else
        echo -e "\033[33m[WARN] Native binary compilation failed. Falling back to direct script execution.\033[0m"
        cd "$SCRIPT_DIR"
    fi
else
    echo "[INFO] CMake not found. Bootstrapping directly via script fallback..."
fi

# Fallback: execute setup_env.py directly
PYTHON_CMD="python3"
if ! command -v python3 >/dev/null 2>&1 && command -v python >/dev/null 2>&1; then
    PYTHON_CMD="python"
fi

if command -v $PYTHON_CMD >/dev/null 2>&1; then
    echo -e "\033[32m[EXEC] Executing Python bootstrap setup script...\033[0m"
    $PYTHON_CMD scripts/setup_env.py
else
    echo -e "\033[31m[ERROR] Python interpreter not found on the system PATH.\033[0m"
    echo -e "\033[33m        Please install Python or configure the environment.\033[0m"
    exit 1
fi
