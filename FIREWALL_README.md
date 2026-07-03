# DJ MIDI Watts - Firewall Security Configuration

This document outlines the firewall rules, permissions, and exceptions implemented to secure the DJ MIDI Watts application on your local network.

## The DJ Grid Architecture

DJ MIDI Watts uses a local "Grid" architecture to communicate between your desktop software, web UI, mobile app, and browser extensions. This requires certain network ports to be open for inbound connections.

### Bound Ports

- **80**: Standard HTTP (Legacy / Fallback Hub)
- **5555**: Android Debug Bridge (ADB) / Local TCP Socket
- **8080**: WebUI Development Server / Main Hub
- **8081**: Secondary Data Stream

## Security Lockdown Policy

To ensure that your software is not exposed to external threats, the `scripts/setup_firewall.ps1` script enforces a strict security policy:

### 1. Private Profiles Only

The exceptions are bound strictly to the **Private** network profile. 

- **What this means:** If you take your laptop to a coffee shop, airport, or any network designated as "Public" in Windows, the firewall will automatically drop all traffic to these ports, protecting you from public snoopers.

### 2. Local Subnet Scope

The exceptions utilize the `-RemoteAddress LocalSubnet` parameter.

- **What this means:** Only devices that share your exact local router/switch (e.g., your smartphone connected to your home Wi-Fi) can communicate with the Grid. External traffic attempting to hit your public IP address will be instantly rejected.

## How to Apply Rules

The firewall configuration requires Administrator permissions to modify Windows Defender. 

1. Open a new **PowerShell** terminal as **Administrator**.
2. Navigate to the DJ MIDI Watts directory.
3. Run the setup script:

   ```powershell
   .\scripts\setup_firewall.ps1
   ```

4. You will receive a security notification prompting you to type `Y` or `Yes` to authorize the strict lockdown policy.

By enforcing these constraints, DJ MIDI Watts operates seamlessly across your personal devices without compromising your machine's security boundary.
