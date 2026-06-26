@echo off
echo ===================================================
echo   Compiling Native Hardware Manager using MSYS2 G++
echo ===================================================
IF NOT EXIST ..\bin mkdir ..\bin
C:\msys64\ucrt64\bin\g++.exe -g main.cpp -o ..\bin\hardware_manager.exe -lz
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Binary compiled to desktop/bin/hardware_manager.exe
) else (
    echo [ERROR] Compilation failed. Ensure MSYS2 ucrt64 and zlib are installed.
)
