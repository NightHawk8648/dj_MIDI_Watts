# DJ MIDI WATTS - VPN and Remote Tunneling Guide

This guide details how to set up, operate, and secure the remote synchronization capabilities of DJ MIDI WATTS using an encrypted mesh VPN.

---

## 🌐 Why a Mesh VPN?

Opening standard router ports (such as ports `80`, `443`, or `8080`) to access your home MIDI server remotely poses severe security risks. Attackers can scan these open ports to exploit software vulnerabilities or hijack your local machine.

We utilize **Tailscale**, a zero-config, highly-secure encrypted mesh VPN based on the modern WireGuard® protocol. Tailscale assigns a permanent, private IP address to each connected device and encrypts all peer-to-peer traffic natively.

---

## 🛠️ Step-by-Step Tunneling Setup

1. **Sign Up**: Register for a free account at [Tailscale.com](https://tailscale.com).
2. **Install on Server**:
   Run the following installation command in your physical or virtual server terminal:

   ```bash
   curl -fsSL https://tailscale.com/install.sh | sh
   sudo tailscale up
   ```

3. **Authorize**: Open the unique link printed in the terminal to register the server device to your Tailscale network.
4. **Install on Clients**: Install the Tailscale application on your main workstation, laptop, or mobile Android device. Log into the same Tailscale account.
5. **Map internal IP**: Copy the unique Tailscale IP address assigned to your server (e.g., `100.x.x.x`).
6. **Activate Sync**: Open the DJ MIDI WATTS extension dashboard, toggle **Enable Personal Cloud**, and enter your server's Tailscale IP address.

---

## 🔒 Encryption & Data Masking

While connected over the Tailscale VPN tunnel:

- All communications are fully encrypted end-to-end using WireGuard's modern cryptographic primitives.
- Network credentials and OAuth tokens are never exposed in plaintext telemetry logs.
- The application automatically masks files paths and active user directories in logs (e.g., swapping `C:\Users\Username` with `C:\Users\***`).

---

## ⚠️ Security Risk Warning

> [!WARNING]
> **CRITICAL SECURITY RISK WARNING:**
> If you choose to opt out of the decentralized Personal Cloud setup and choose not to configure Tailscale VPN, all remote synchronization and API request paths will be exposed on the open network.
>
> **YOUR INFORMATION MIGHT BE AT RISK.** Operating in an un-tunneled network configuration allows malicious actors to monitor your API traffic, compromise your API keys, and target your local system registry. Setting up the mesh VPN is highly recommended to protect your credentials and maintain "Grid Sovereignty".
