# Privacy Policy

**Effective Date:** 2026-06-29

## 1. Introduction
Welcome to DJ MIDI Watts. This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you use our Chrome Extension, Android Application, Desktop software, and related services. We are committed to respecting your privacy and protecting your personal data, operating entirely on a decentralized "Grid" architecture.

## 2. Data Collection and Usage
### 2.1 Local Network Telemetry & Grid Architecture
DJ MIDI Watts is designed as a localized, decentralized Grid system. Hardware telemetry, MIDI maps, FX presets, and device libraries are transmitted exclusively over your immediate Local Subnet (e.g., your home Wi-Fi) between your personal devices via bound ports (8080, 8081, 80, 5555). We do not transmit this data to central external servers unless you explicitly configure a self-hosted cloud endpoint.

### 2.2 Google API & OAuth2
Our Chrome Extension and Android app use Google OAuth2 (`identity`) for optional cross-profile syncing of your custom setups.
- **Data Accessed:** We only request access to your `email` and standard Google identity profile to securely link your devices.
- **Data Sharing:** We do not sell, rent, or trade your Google data. It is strictly used to authenticate your session against your personal configuration.

### 2.3 Browser Extension Permissions
- **declarativeNetRequest & activeTab**: Used strictly locally to inject non-intrusive WebGL visual effects into your active music-playing tabs, and to prevent third-party trackers from causing latency during your live mix. We do not track your browsing history.
- **desktopCapture**: Used entirely locally (via WebRTC) to stream visualizer outputs to your Grid. Your screen is never broadcasted to external servers.

## 3. Data Storage
Your data, including audio configurations, converted media (e.g., FLAC, WAV, MP4), and visual preferences, are stored locally on your device or on your personally configured Self-Hosted Cloud / Grid Hub. We do not maintain a centralized database of user activity.

## 4. Security
We prioritize your security by enforcing a Strict Lockdown Policy. Grid ports are restricted to `LocalSubnet` via `Private` firewall profiles to prevent external tampering or data breaches on public networks.

## 5. Your Rights
Because DJ MIDI Watts relies on local storage and personal cloud solutions, you maintain complete "Grid Sovereignty." You can delete all your data instantly by clearing your browser's local storage and uninstalling the applications.

## 6. Contact Us
If you have questions or comments about this Privacy Policy, please contact the developer via our official repository.
