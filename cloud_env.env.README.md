# DJ MIDI WATTS - Cloud Environment Configuration Guide (`cloud_env.env`)

This document explains the cloud deployment structure, secrets isolation strategy, and masking mechanisms implemented in the DJ MIDI WATTS project.

---

## 🛠️ Overview of `cloud_env.env`

The `cloud_env.env` file acts as the configuration hub for cloud storage, services, and general infrastructure variables. It dictates target project identifiers, deployment environments, endpoints, and settings for Google Cloud Platform.

### Core Variables Defined

- **GCP_PROJECT_ID**: Target Google Cloud Project ID.
- **GCP_REGION**: Cloud location (e.g., `YOUR_REGION`).
- **GCP_STORAGE_BUCKET**: Buckets for user assets, libraries, and application cache storage.
- **GCP_CLOUD_RUN_URL**: Endpoint routing API queries dynamically to the server backend (Future Implementation).

---

## 🔒 Separation of Credentials

To protect individual developer identities, the credentials have been segregated:

1. **User Personal Credentials**: Managed in `user.env` (templated from `user.env.example`).
2. **Infrastructure Environment**: Stored in `cloud_env.env`.

> [!NOTE]
> All personal files (`user.env` and `cloud_env.env`) are listed in `.gitignore` and are omitted from public commits.

---

## 🎭 Masking & Secret Protection

When the application compiles or is run locally, secrets undergo multi-tier masking to prevent exposure:

- **Registry/Environment Masking**: Custom functions (`Get-MaskedValue`) hide keys visually by showing only the first and last four characters (e.g., `AQ.A...F0Pg`), or `********` for short strings.
- **Dual-Layer TS (Timestamp + Signature)**: Transmitted settings use a secure SHA-256 fingerprint hash paired with a secure timestamp signature to validate legitimacy without exposing raw values.
- **Secure Vault Handshake**: Secrets can be cached inside the operating system's native secure credential store (Windows Vault) using:

  ```powershell
  .\scripts\sync_gcp_secrets.ps1
  ```

  Once stored in the vault, local plaintext representations of these credentials can be safely deleted.

---

## ⚠️ Security Risk Warning

> [!WARNING]
> **CRITICAL DATA EXPOSURE RISK:**
> If you choose not to deploy a self-hosted cloud server (Option A: dedicated server or Option B: Virtual Machine) and opt not to use the Tailscale VPN mesh network, your API keys, local credentials, and device state logs will be routed without encrypted tunneling.
>
> **YOUR INFORMATION MIGHT BE AT RISK.** Hackers could intercept API requests, trace your credentials, or gain unauthorized access to your MIDI controller grid. Ensure Tailscale and Nextcloud are properly active to eliminate these risks.
