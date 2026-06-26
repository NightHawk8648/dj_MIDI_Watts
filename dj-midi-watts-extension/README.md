# DJ MIDI WATTS Browser Extension (Build Skeleton)

A responsive high-fidelity Manifest V3 control deck that connects directly to the running `http://localhost:8080` API server hosted on your Android application or emulator.

---

## Supported Browsers

* **Google Chrome** (v88+)
* **Mozilla Firefox** (v109+)
* **Microsoft Edge**
* **Opera & Opera GX**

---

## 🛠️ Loading the Extension

### Chrome, Edge, and Opera (Chromium-based)

1. Open your browser and navigate to the extensions control page:

   * **Chrome**: `chrome://extensions/`
   * **Edge**: `edge://extensions/`
   * **Opera**: `opera://extensions/`
2. **Enable "Developer Mode"** using the toggle switch in the top-right corner.
3. Click the **"Load unpacked"** button in the top-left corner.
4. Select the `/dj-midi-watts-extension` folder containing `manifest.json`.
5. Pin the extension to your toolbar. Clicking on it opens the controller deck panel!

### Mozilla Firefox

1. Open Firefox and enter `about:debugging` in the address bar.
2. Select **"This Firefox"** from the left-hand navigation sidebar.
3. Scroll down and click **"Load Temporary Add-on..."**.
4. Select the `manifest.json` file inside the `dj-midi-watts-extension` folder.
5. Firefox attaches the extension instantly!

---

## 📡 Dynamic API Sync Channels

The extension hooks into four core RESTful endpoints:

* `GET /api/state` - Fetch current faders, strobe status, preset logs, and track status.
* `GET /api/control?param=<name>&value=<val>` - Wire low/mid/high frequency EQ levels dynamically.
* `GET /api/trigger?fx=<name>` - Trigger instantaneous lasers/strobe bursts/smoke triggers.
* `GET /api/preset/apply?id=<preset_id>` - Synchronize global snaps instantly.
