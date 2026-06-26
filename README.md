# DJ‑MIDI‑WATTS  
Universal Audio & MIDI Platform • Modern + Vintage Device Support • AI‑Enhanced

DJ‑MIDI‑WATTS is a universal, extensible audio and MIDI platform designed to support any modern or vintage MIDI controller, synthesizer, equalizer, DJ controller, audio interface, or performance device. The platform includes a modular device library system, firmware and driver management, customizable GUI themes, AI‑assisted audio tools, and beginner‑friendly tutorials.

This repository contains the core application, documentation, specifications, and deployment metadata for the DJ‑MIDI‑WATTS ecosystem.

---

## 🚀 Features

### **Universal Device Support**
- USB‑MIDI, 5‑pin DIN MIDI, Serial MIDI  
- Vintage synths (Roland, Yamaha, Korg, Akai, etc.)  
- Modern DJ controllers and audio interfaces  
- Auto‑mapping + downloadable device libraries  
- Firmware + driver update system  

### **Customizable GUI**
- Drag‑and‑drop panels  
- Resizable modules  
- Free + premium themes  
- Virtual controllers (knobs, faders, pads, synth modules)  

### **AI‑Powered Audio Tools**
- EQ suggestions  
- Controller mapping optimization  
- Sound design recommendations  
- Microphone/headphone tuning  
- Smart presets  

### **Beginner‑Friendly**
- Guided onboarding  
- Interactive tutorials  
- Default beginner presets  
- “Explain this setting” tooltips  

---

## 📄 Documentation

### **PDF Documentation Package**
Contains:
- General Description  
- Version Entry (v2.1.0)  
- Specification File  
- Deployment Link  
- Additional Attributes  

**Download:**  
`/DJ-MIDI-WATTS-Documentation-v2.1.0.pdf`

### **Deployment Link**
https://deployment.example.com/midi-watts/v2.1.0

---

## 🧩 Specifications

### **Spec Types**
- OpenAPI 3.1  
- AsyncAPI 3.0  
- JSON Schema 2020‑12  

Specification files are located in:  
`/spec/`

---

## 🗂 Version Metadata

```json
{
  "apiId": "MIDI-WATTS-API",
  "displayName": "MIDI-WATTS API",
  "versionId": "v2.1.0",
  "description": "Universal audio and MIDI platform API for device libraries, presets, AI suggestions, and configuration.",
  "releaseDate": "2026-06-01",
  "stability": "stable"
}

## 🔒 Security & Credentials Isolation

To ensure maximum security and privacy, **DJ‑MIDI‑WATTS** isolates personal API keys and credentials from core codebase files. 

### **Credentials Configuration (`user.env`)**
All user-specific configurations and credentials must be stored in `user.env` at the root of the project.
1. Copy [user.env.example](file:///c:/Users/Night/dj_MIDI_Watts/user.env.example) to create your local `user.env`:
   ```bash
   cp user.env.example user.env
   ```
2. Open `user.env` and insert your personal API keys and Google Cloud Project identifiers.
3. For enhanced protection, run the provisioning utility to cache keys directly in your operating system's secure vault:
   ```powershell
   .\scripts\sync_gcp_secrets.ps1
   ```
   Once synced, local plaintext secrets can be safely cleared from your configuration files.

> [!WARNING]
> **CRITICAL SECURITY RISK NOTICE:**
> If you opt NOT to use the **Create Your Own Cloud** option (Nextcloud hosting) AND opt NOT to use the **Tailscale VPN** mesh tunnel, your personal API keys, telemetry packets, and device cache files will be transmitted without proper decentralized encapsulation over the public network. **YOUR SENSITIVE INFORMATION MIGHT BE AT RISK OF INTERCEPTION OR EXPOSURE.** 
> Setting up both Tailscale and a self-hosted cloud is highly recommended to guarantee "Grid Sovereignty".

---

## 🎧 Self-Hosting: Your Personal DJ Midi Watts Cloud

DJ Midi Watts supports a completely decentralized, privacy-first infrastructure. You can optionally host your own custom cloud server to manage and sync binaries, libraries, logs, and application cache per device, keeping your data entirely under your control and free from centralized hosting costs.

# # 🎧 Welcome to your Personal DJ Midi Watts Cloud Setup!

By following this guide, you will build a completely free, 100% private, and highly secure cloud infrastructure to operate and store your libraries, logs, binaries, and application cache. Your data stays completely hidden from the public internet.

---

### Phase 1: Prepare Your Server Hardware

You have two choices for provisioning your underlying server environment:

#### Option A: Dedicated Physical Hardware
1. Grab a spare computer or laptop (a stable, lightweight Linux distribution like Debian or Ubuntu Server is highly recommended for optimal performance).
2. Ensure it is powered on and connected constantly to your local home network.

#### Option B: Virtual Machine (VM) Deployment
1. If you prefer not to dedicate physical hardware, you can spin up a new Virtual Machine (VM) utilizing hypervisors like VirtualBox, VMware, Proxmox, or Hyper-V.
2. Allocate a minimum of 2 vCPUs, 2GB of RAM, and a dynamic virtual hard disk using a Debian or Ubuntu Server ISO image.
3. Ensure the VM's network adapter is set to **Bridged Mode** so it receives its own IP address directly from your local network router.
### Phase 2: Run the Storage Installation Command

Open the Terminal on your server machine and paste the following commands to provision the storage layers:

```bash
# Update repositories and install Snap management tools
\'\'\'sudo apt update && sudo apt install snapd -y
sudo snap install core''
\'\'\'

# Deploy the cloud core system
sudo snap install nextcloud

#### Note: Once finished, open a browser on your main computer and navigate to your server's IP address (e.g., http://192.168.1.50) to create your master administrator account.

#### Phase 3: Lock Down Security (The Hidden Tunnel)
To access your DJ Midi Watts files remotely from your web extension or Android application without dangerously opening router ports to hackers, we use an encrypted mesh VPN called Tailscale.

Go to Tailscale's website and sign up for a free personal account.

Install Tailscale on your physical machine or virtual machine by running:

```bash
\'\'\'
curl -fsSL [https://tailscale.com/install.sh](https://tailscale.com/install.sh) | sh
   sudo tailscale up''
\'\'\'

 1. Click the link provided in the terminal to authorize the machine.

 2. Install the Tailscale app on your Android phone, tablet, or main desktop machine and log into the same account.

 3.Copy the special, permanent internal IP address (e.g., 100.x.x.x) assigned to your server.

#### Phase 4: Link It to the DJ Midi Watts Application
Open the DJ Midi Watts application/extension configuration dashboard.

Toggle "Enable Personal Cloud" to ON.

Paste your server's Tailscale IP (100.x.x.x), your username, and your Nextcloud App Password (generated in Nextcloud Settings > Security > App Passwords).

Click Test & Sync. Your workspace, application binaries, caches, and runtime logs will now sync completely automatically across all your devices!

