# DJ MIDI WATTS - Driver Setup & Automated Loopback Installer Utility
# This script downloads, extracts, and executes driver installers for virtual MIDI and serial routing.

$ErrorActionPreference = "Stop"
Clear-Host

Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "         DJ MIDI WATTS - MIDI/SERIAL DRIVER SETUP UTILITY" -ForegroundColor Cyan -Bold
Write-Host "=====================================================================" -ForegroundColor Cyan

# 1. Administrator Check
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Warning "This installer requires Administrator privileges to run system driver setups."
    Write-Host "Please restart your PowerShell console as Administrator and re-run this script." -ForegroundColor Red
    exit 1
}

# 2. Paths
$tempDir = Join-Path $PSScriptRoot "installers"
if (-not (Test-Path $tempDir)) {
    New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
}

$loopMidiZip = Join-Path $tempDir "loopMIDI.zip"
$loopMidiExtract = Join-Path $tempDir "loopMIDI_extracted"
$loopMidiUrl = "https://www.tobias-erichsen.de/wp-content/uploads/2020/01/loopMIDI_1_0_16_27.zip"

# Check if loopMIDI already installed
$loopMidiPath = "C:\Program Files (x86)\Tobias Erichsen\loopMIDI\loopMIDI.exe"
if (-not (Test-Path $loopMidiPath)) {
    $loopMidiPath = "C:\Program Files\Tobias Erichsen\loopMIDI\loopMIDI.exe"
}

# 3. Installing loopMIDI
if (Test-Path $loopMidiPath) {
    Write-Host "[OK] loopMIDI driver is already installed on the system." -ForegroundColor Green
} else {
    Write-Host "[INFO] loopMIDI is missing. Commencing automated download..." -ForegroundColor Yellow
    Write-Host "Downloading loopMIDI zip from Tobias Erichsen official server..." -ForegroundColor Gray
    
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $loopMidiUrl -OutFile $loopMidiZip -UseBasicParsing
        Write-Host "[OK] Download completed successfully." -ForegroundColor Green
        
        Write-Host "Extracting installer archive..." -ForegroundColor Gray
        if (Test-Path $loopMidiExtract) {
            Remove-Item -Recurse -Force $loopMidiExtract | Out-Null
        }
        Expand-Archive -Path $loopMidiZip -DestinationPath $loopMidiExtract -Force
        
        $msiFile = Get-ChildItem -Path $loopMidiExtract -Filter "*.msi" | Select-Object -First 1
        if ($msiFile) {
            Write-Host "[INFO] Launching loopMIDI Installer: $($msiFile.Name)..." -ForegroundColor Cyan
            Write-Host "Please complete the setup GUI wizard that appears." -ForegroundColor Gray
            Start-Process -FilePath "msiexec.exe" -ArgumentList "/i `"$($msiFile.FullName)`"" -Wait
            Write-Host "[OK] Installer finished execution." -ForegroundColor Green
        } else {
            $exeFile = Get-ChildItem -Path $loopMidiExtract -Filter "*.exe" | Select-Object -First 1
            if ($exeFile) {
                Write-Host "[INFO] Launching loopMIDI Installer: $($exeFile.Name)..." -ForegroundColor Cyan
                Start-Process -FilePath $exeFile.FullName -Wait
                Write-Host "[OK] Installer finished execution." -ForegroundColor Green
            } else {
                throw "No installer payload (.exe or .msi) found in the zip archive."
            }
        }
    } catch {
        Write-Host "[ERROR] Failed to install loopMIDI: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Please download loopMIDI manually from: https://www.tobias-erichsen.de/software/loopmidi.html" -ForegroundColor Yellow
    }
}

# 4. USB-to-Serial Driver Support info
Write-Host "`nChecking USB-to-Serial driver support (for Serial DMX connectivity)..." -ForegroundColor Gray

# Common USB-to-Serial chips: CH340, FTDI FT232, CP2102, PL2303
Write-Host "System references for hardware drivers:" -ForegroundColor Gray
Write-Host "  - FTDI (DMX Pro / OpenDMX): https://ftdichip.com/drivers/vcp-drivers/" -ForegroundColor Gray
Write-Host "  - CH340 (Arduino-based DMX): http://www.wch-ic.com/downloads/CH341SER_EXE.html" -ForegroundColor Gray
Write-Host "  - Silicon Labs CP210x: https://www.silabs.com/developers/usb-to-uart-bridge-vcp-drivers" -ForegroundColor Gray

# 5. Bluetooth/BLE MIDI Support Info
Write-Host "`nChecking Bluetooth/BLE MIDI driver support..." -ForegroundColor Gray
Write-Host "Windows 10/11 includes built-in MIDI over Bluetooth Low Energy (BLE) support." -ForegroundColor Gray
Write-Host "If you have a hardware Bluetooth MIDI controller (e.g. Korg microKEY Air, Yamaha MD-BT01):" -ForegroundColor Gray
Write-Host "  1. Pair the device via Windows Settings -> Bluetooth & Devices." -ForegroundColor Gray
Write-Host "  2. The MIDI ports should automatically register as standard MIDI inputs." -ForegroundColor Gray
Write-Host "  3. If your DAW doesn't support UWP MIDI, download 'midimittr' or helper tools." -ForegroundColor Gray

Write-Host "`n=====================================================================" -ForegroundColor Cyan
Write-Host "Driver setup checks completed." -ForegroundColor Green
Write-Host "Please run .\preboot_device_discovery.ps1 next to update the registry." -ForegroundColor Yellow
Write-Host "=====================================================================" -ForegroundColor Cyan
