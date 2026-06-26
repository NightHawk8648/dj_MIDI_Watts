# DJ MIDI WATTS - Driver Setup & Hardware Sync Guide

Welcome to the **Hardware and Driver Sync Suite** for DJ MIDI WATTS. This guide covers how to set up, initialize, and run drivers for USB-connected controllers, virtual loopback MIDI, and wireless Bluetooth controllers.

---

## 1. Preboot Discovery and Sync System

We have introduced a **Preboot Discovery System** that detects hardware controllers, virtual adapters, and Bluetooth links *before* launching the main system.

To run the preboot discovery script:
1. Open a PowerShell console (as Administrator to allow driver installs/syncs).
2. Execute the preboot script:
   ```powershell
   cd drivers
   .\preboot_device_discovery.ps1
   ```
This updates [device_registry.json](file:///c:/Users/Night/dj_MIDI_Watts/drivers/device_registry.json) with active hardware ports and maps them to the Web UI.

---

## 2. Virtual MIDI Loopback Setup (Windows)

Windows lacks native virtual loopback MIDI routing (unlike macOS's IAC driver). To bridge signals between the **Web UI**, **Python Sync Engine**, and your **DAW** (e.g. Ableton Live, FL Studio, Cubase, Traktor):

### Installation via Installer Script
1. Open PowerShell as Administrator.
2. Run the driver installer utility:
   ```powershell
   .\setup_midi_drivers.ps1
   ```
This will automatically download and extract Tobias Erichsen's **loopMIDI** utility and launch the installer.

### Manual Setup
If you prefer to configure it manually:
1. Download loopMIDI from [Tobias Erichsen's website](https://www.tobias-erichsen.de/software/loopmidi.html).
2. Open loopMIDI.
3. Click the `+` button in the bottom left to create two ports:
   * **`DJ_WATTS_IN`**
   * **`DJ_WATTS_OUT`**
4. Keep loopMIDI running in the background.

---

## 3. Physical USB MIDI Controller Setup

Most modern USB MIDI controllers (keyboards, DJ decks, drum pads) are **Class-Compliant**, meaning Windows automatically installs the necessary drivers when plugged in.

### Specific Manufacturer Drivers
Some vintage or advanced devices require custom drivers. If the Preboot script alerts that a plugged-in USB MIDI controller is missing its driver, download it from the manufacturer:
* **Korg**: [Korg USB-MIDI Driver](https://www.korg.com/us/support/download/driver/0/285/3541/)
* **Roland**: [Roland USB-MIDI Drivers](https://www.roland.com/global/support/by_product/)
* **Yamaha**: [Yamaha USB-MIDI Driver](https://usa.yamaha.com/support/updates/index.html)
* **Akai / Novation**: Usually class-compliant (plug-and-play).

---

## 4. Bluetooth/BLE MIDI Setup (Wireless)

For wireless Bluetooth MIDI keyboards, drum pads, or synthesizers:

1. Enable Bluetooth on your Windows machine.
2. Put your MIDI device into Bluetooth pairing mode.
3. In Windows, go to **Settings > Bluetooth & Devices > Add Device > Bluetooth** and select your controller.
4. Once paired, Windows UWP MIDI API handles communication. If your DAW or software only supports standard WinMM MIDI (and doesn't detect UWP MIDI), download:
   * **midimittr** for Windows
   * Or **loopMIDI** + **Bluetooth MIDI Router** utility.

---

## 5. Serial USB COM Drivers (DMX Stage Sync)

The `sync_engine.py` script routes stage effects (Fader level -> DMX universe). If you are using a USB-to-DMX serial interface, install the virtual COM port (VCP) driver matching your interface's chip:
* **FTDI Chip** (OpenDMX, DMX King, Enttec Pro): [FTDI VCP Drivers](https://ftdichip.com/drivers/vcp-drivers/)
* **CH340/CH341** (Arduino/Clone boards): [WCH Official Drivers](http://www.wch-ic.com/downloads/CH341SER_EXE.html)
* **Silicon Labs CP210x**: [CP210x Drivers](https://www.silabs.com/developers/usb-to-uart-bridge-vcp-drivers)

---

## 6. DAW Integration & Routing Matrix

Once ports are created, map them inside your Digital Audio Workstation:

### Ableton Live:
* Open **Preferences > MIDI**.
* Under **MIDI Ports**:
  * Track **In: DJ_WATTS_IN** -> **ON**
  * Remote **In: DJ_WATTS_IN** -> **ON**
  * Track **Out: DJ_WATTS_OUT** -> **ON**
  * Remote **Out: DJ_WATTS_OUT** -> **ON**

### FL Studio:
* Open **Options > MIDI Settings**.
* Enable **DJ_WATTS_IN** (Input port) and map it to an unused port number (e.g. port `1`).
* Enable **DJ_WATTS_OUT** (Output port) and map it to port `1`.
