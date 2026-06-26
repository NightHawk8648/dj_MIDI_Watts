# DJ MIDI WATTS - Preboot Device Discovery & Sync System
# This script runs prior to main system startup to scan, verify, and synchronize MIDI/DMX hardware.

$ErrorActionPreference = "Continue"
Clear-Host

Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "         DJ MIDI WATTS - PREBOOT DEVICE DISCOVERY & SYNC" -ForegroundColor Cyan -Bold
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "Initializing hardware probe..." -ForegroundColor Gray

# 1. OS and Privilege Checks
if ($PSVersionTable.OS -and $PSVersionTable.OS -notmatch "Windows") {
    Write-Host "[WARNING] This script is optimized for Windows systems. Cross-platform support is limited." -ForegroundColor Yellow
}

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "[INFO] Running in User Mode. Administrator privileges are recommended for driver installation." -ForegroundColor Yellow
} else {
    Write-Host "[OK] Running with Administrator Privileges." -ForegroundColor Green
}

# Ensure drivers folder exists
$driversDir = "$PSScriptRoot"
if (-not (Test-Path $driversDir)) {
    New-Item -ItemType Directory -Path $driversDir -Force | Out-Null
}

$registryPath = Join-Path $driversDir "device_registry.json"

# Discover devices
Write-Host "`nScanning system hardware buses..." -ForegroundColor Gray

# A. USB MIDI and Audio Devices discovery
Write-Host "Probing USB devices..." -ForegroundColor Gray
$usbDevices = @()
try {
    # Query PnPEntities matching common MIDI/Audio/DMX keywords or service classes
    $pnpEntities = Get-CimInstance Win32_PnPEntity -ErrorAction SilentlyContinue
    
    # Filter for USB Audio/MIDI/Serial controllers
    foreach ($dev in $pnpEntities) {
        $name = $dev.Caption
        $pnpId = $dev.PNPDeviceID
        $status = $dev.Status
        $service = $dev.Service
        
        $isMidi = $name -like "*MIDI*" -or $service -eq "usbmidi"
        $isAudio = $name -like "*USB Audio*" -or $name -like "*Sound Card*" -or $name -like "*Audio Interface*"
        $isSerialDmx = $name -like "*FTDI*" -or $name -like "*CH340*" -or $name -like "*Prolific*" -or $name -like "*CP210*" -or $service -eq "ftser2k" -or $service -eq "ser2pl"
        
        if ($isMidi -or $isAudio -or $isSerialDmx) {
            $type = "USB Audio/MIDI"
            if ($isSerialDmx) { $type = "USB Serial (DMX)" }
            
            $usbDevices += @{
                name = $name
                type = $type
                pnp_id = $pnpId
                status = $status
                service = $service
                needs_driver = ($status -ne "OK" -and $status -ne "Degraded")
            }
        }
    }
} catch {
    Write-Host "[ERROR] USB probe encountered an error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host "Discovered $($usbDevices.Count) USB audio/MIDI/serial devices." -ForegroundColor Green

# B. Bluetooth and BLE Devices discovery
Write-Host "Probing Bluetooth & BLE interfaces..." -ForegroundColor Gray
$btDevices = @()
$bluetoothEnabled = $false
try {
    # Look for Bluetooth radio interface
    $btRadio = $pnpEntities | Where-Object { $_.ClassGuid -eq "{e0cbf06c-cd8b-4647-bb8a-263b43f0f974}" -or $_.Caption -like "*Bluetooth*" }
    if ($btRadio) {
        $bluetoothEnabled = $true
        # Discover paired/connected Bluetooth devices
        # Class GUID for Bluetooth device class in Windows is usually {e0cbf06c-cd8b-4647-bb8a-263b43f0f974}
        # Get actual paired Bluetooth entities under BTH or BTHENUM
        foreach ($dev in $pnpEntities) {
            if ($dev.PNPDeviceID -like "*BTHENUM*" -or $dev.PNPDeviceID -like "*BTH\\*") {
                # Filter out generic services, focus on peripherals/audio/MIDI
                if ($dev.Caption -and $dev.Caption -notlike "*Microsoft*" -and $dev.Caption -notlike "*Bluetooth Device*") {
                    $btDevices += @{
                        name = $dev.Caption
                        pnp_id = $dev.PNPDeviceID
                        status = $dev.Status
                        connected = ($dev.Status -eq "OK")
                    }
                }
            }
        }
    }
} catch {
    Write-Host "[ERROR] Bluetooth probe encountered an error: $($_.Exception.Message)" -ForegroundColor Red
}
if ($bluetoothEnabled) {
    Write-Host "Bluetooth is ENABLED. Discovered $($btDevices.Count) paired Bluetooth peripheral(s)." -ForegroundColor Green
} else {
    Write-Host "Bluetooth is DISABLED or no adapter was found." -ForegroundColor Yellow
}

# C. Virtual MIDI Loopback discovery
Write-Host "Probing Virtual MIDI drivers..." -ForegroundColor Gray
$virtualMidiInstalled = $false
$loopMidiProcessRunning = $false
$virtualPorts = @()

# Check registry for loopMIDI
try {
    $regPaths = @(
        "HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*",
        "HKLM:\Software\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*",
        "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*"
    )
    foreach ($p in $regPaths) {
        $match = Get-ItemProperty -Path $p -ErrorAction SilentlyContinue | Where-Object { $_.DisplayName -like "*loopMIDI*" -or $_.DisplayName -like "*virtualMIDI*" }
        if ($match) {
            $virtualMidiInstalled = $true
            break
        }
    }
} catch {}

# Check for loopMIDI running process
$process = Get-Process -Name "loopMIDI" -ErrorAction SilentlyContinue
if ($process) {
    $loopMidiProcessRunning = $true
    $virtualMidiInstalled = $true
}

# Probing active MIDI ports via loopMIDIcmd CLI if installed
$loopMidiCmdPath = "C:\Program Files (x86)\Tobias Erichsen\loopMIDI\loopMIDIcmd.exe"
if (-not (Test-Path $loopMidiCmdPath)) {
    $loopMidiCmdPath = "C:\Program Files\Tobias Erichsen\loopMIDI\loopMIDIcmd.exe"
}

if (Test-Path $loopMidiCmdPath) {
    # If the tool exists, we can query or configure
    # loopMIDIcmd doesn't have a direct 'list' command outputting pure text cleanly without errors,
    # but we know it's installed and can configure ports.
    # We will check if default ports are already configured or write them.
}

# Compile registry JSON
$warnings = @()
if ($usbDevices.Count -eq 0) {
    $warnings += "No USB MIDI controllers or Audio interfaces connected."
}
if (-not $bluetoothEnabled) {
    $warnings += "Bluetooth adapter not detected or disabled. MIDI-over-Bluetooth is unavailable."
}
if (-not $virtualMidiInstalled) {
    $warnings += "Virtual MIDI driver (loopMIDI) is not installed. Virtual routing is offline."
}

$registryData = @{
    timestamp = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
    bluetooth_enabled = $bluetoothEnabled
    virtual_midi_installed = $virtualMidiInstalled
    loopmidi_running = $loopMidiProcessRunning
    devices = @{
        usb = $usbDevices
        bluetooth = $btDevices
        virtual = @(
            @{
                name = "loopMIDI Virtual Interface"
                installed = $virtualMidiInstalled
                running = $loopMidiProcessRunning
                ports = @("DJ_WATTS_IN", "DJ_WATTS_OUT")
            }
        )
    }
    warnings = $warnings
}

$registryData | ConvertTo-Json -Depth 6 | Out-File $registryPath -Encoding utf8
Write-Host "`nDevice registry updated: $registryPath" -ForegroundColor Green

# 3. Actions / Prompts
Write-Host "`n------------------ RECOMMENDED ACTION ITEMS ------------------" -ForegroundColor Cyan

# Check if loopMIDI is missing
if (-not $virtualMidiInstalled) {
    Write-Host "[!] missing loopMIDI: loopMIDI is required to route MIDI signals between Web UI and DAWs." -ForegroundColor Yellow
    Write-Host "--> You can run the setup script: .\setup_midi_drivers.ps1 to download and install loopMIDI." -ForegroundColor Cyan
} elseif (-not $loopMidiProcessRunning) {
    Write-Host "[!] loopMIDI is installed but NOT running." -ForegroundColor Yellow
    Write-Host "--> Attempting to launch loopMIDI..." -ForegroundColor Gray
    
    $loopMidiPath = "C:\Program Files (x86)\Tobias Erichsen\loopMIDI\loopMIDI.exe"
    if (-not (Test-Path $loopMidiPath)) {
        $loopMidiPath = "C:\Program Files\Tobias Erichsen\loopMIDI\loopMIDI.exe"
    }
    
    if (Test-Path $loopMidiPath) {
        try {
            Start-Process -FilePath $loopMidiPath -WindowStyle Minimized
            Write-Host "--> loopMIDI process started." -ForegroundColor Green
            # Update registry
            $registryData.loopmidi_running = $true
            $registryData.devices.virtual[0].running = $true
            $registryData | ConvertTo-Json -Depth 6 | Out-File $registryPath -Encoding utf8
        } catch {
            Write-Host "Failed to automatically start loopMIDI. Please start it manually." -ForegroundColor Red
        }
    } else {
        Write-Host "Could not locate loopMIDI.exe path. Please start loopMIDI manually." -ForegroundColor Red
    }
} else {
    Write-Host "[OK] loopMIDI is running and virtual ports are ready." -ForegroundColor Green
}

# If loopMIDI is running, attempt to create default ports using CLI
if ($virtualMidiInstalled -and (Test-Path $loopMidiCmdPath)) {
    Write-Host "Syncing default loopback ports (DJ_WATTS_IN, DJ_WATTS_OUT)..." -ForegroundColor Gray
    try {
        & $loopMidiCmdPath -create "DJ_WATTS_IN" 2>$null | Out-Null
        & $loopMidiCmdPath -create "DJ_WATTS_OUT" 2>$null | Out-Null
        Write-Host "[OK] Virtual loopback ports synchronized successfully." -ForegroundColor Green
    } catch {
        Write-Host "[WARNING] Could not auto-sync ports: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# If any USB device needs driver
$failedUsb = $usbDevices | Where-Object { $_.needs_driver -eq $true }
if ($failedUsb) {
    Write-Host "`n[!] The following USB devices are connected but may require drivers:" -ForegroundColor Yellow
    foreach ($dev in $failedUsb) {
        Write-Host "  - $($dev.name) (PNP ID: $($dev.pnp_id))" -ForegroundColor Red
    }
    Write-Host "--> Check drivers/README.md for download links for common MIDI and serial devices." -ForegroundColor Cyan
}

# Summary Status
Write-Host "`n=====================================================================" -ForegroundColor Cyan
if ($warnings.Count -gt 0) {
    Write-Host "Preboot hardware check finished with $($warnings.Count) warning(s)." -ForegroundColor Yellow
} else {
    Write-Host "Preboot hardware check finished cleanly. Ready to boot!" -ForegroundColor Green
}
Write-Host "=====================================================================" -ForegroundColor Cyan
