# Chrome Web Store - Permission Justifications

When submitting the DJ MIDI Watts extension to the Chrome Web Store, you will be prompted to justify the permissions requested in your `manifest.json`. Copy and paste the following responses into the developer dashboard:

## `declarativeNetRequest`

**Justification**: This permission is strictly used to block known third-party tracking scripts and session replay tools (e.g., Hotjar, Facebook Pixel, Google Analytics) from executing while the user is actively mixing audio. This ensures a zero-interruption, privacy-first environment for musicians, preventing tracker-induced UI stuttering and network latency during live performances.

## `desktopCapture`

**Justification**: Used exclusively for capturing the screen or application window of a connected visualizer or VJ software to stream visual elements to the local DJ MIDI Watts hardware grid over WebRTC. The extension does not record, store, or transmit the user's screen outside of their local network.

## `activeTab`

**Justification**: Required to inject our non-intrusive WebGL visual effects layer (e.g., laser sweeps, fog, strobe effects) into the user's currently active tab (such as a music streaming service). We use `activeTab` to ensure we only have temporary access to the specific tab the user interacts with, minimizing our footprint and protecting user privacy.

## `identity` (and `oauth2`)

**Justification**: Used to authenticate the user securely against our backend APIs using Google OAuth2. This allows users to seamlessly sync their custom MIDI maps, FX presets, and device libraries across their Chrome profile without relying on third-party tracking cookies.

## `storage` & `alarms`

**Justification**: `storage` is used to locally cache the user's visual FX preferences and network configuration. `alarms` is used to trigger periodic background telemetry pings to our secure Google Cloud API. All connections are routed through a Google Cloud Network Connectivity Hub into a protected Virtual Private Cloud (VPC) via strict cloud network settings, ensuring a secure and encrypted connection to the backend telemetry grid.

## `scripting` & `tabs`

**Justification**: Required to execute our `content.js` script which applies visual FX overlays based on MIDI hardware input (such as bass drops or filter sweeps). We also use `tabs` to check the URL domain (e.g., `window.location.href`) strictly for safety safeguards—ensuring we immediately abort execution on banking sites or competing web MIDI platforms to prevent conflicts or security risks.
