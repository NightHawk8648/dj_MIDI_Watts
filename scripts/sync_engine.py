import os
import sys

# Force standard streams to use UTF-8 to prevent UnicodeEncodeError on Windows
if sys.platform.startswith('win'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except AttributeError:
        pass

import requests
import time
import json
import argparse
import signal
import subprocess
import math
import random
try:
    from dotenv import load_dotenv
except ImportError:
    # Fallback if dotenv is not yet installed
    load_dotenv = lambda: None

try:
    import serial
    from pyartnet import ArtNetNode
    import asyncio
except ImportError:
    serial = None
    ArtNetNode = None

# Patch system path to ensure relative imports and file access work correctly
script_dir = os.path.dirname(os.path.abspath(__file__))
if script_dir not in sys.path:
    sys.path.insert(0, script_dir)

SYNC_INTERVAL = 0.5  # Seconds

# Load environment variables from .env file
load_dotenv()

def get_ultima_credential(key_name, fallback_env=None):
    """Retrieves credentials from Environment or Windows Vault."""
    # 1. Check Primary Environment
    val = os.environ.get(key_name)
    if val and "VOID" not in val: return val
    
    # 2. Check Global Fallback Environment
    if fallback_env:
        val = os.environ.get(fallback_env)
        if val and "VOID" not in val: return val

    # 3. Check Windows Vault via PowerShell Handshake
    try:
        ps_cmd = f"(New-Object Windows.Security.Credentials.PasswordVault).Retrieve('UltimaGrid', '{key_name}').Password"
        result = subprocess.check_output(["powershell", "-NoProfile", "-Command", ps_cmd], 
                                         stderr=subprocess.DEVNULL, text=True).strip()
        if result: return result
    except Exception:
        pass
    return None

class SyncModule:
    def __init__(self, host_url, offline=False, dmx_mode="none", dmx_config=None):
        self.running = True
        self.host_url = host_url
        self.offline = offline
        self.dmx_mode = dmx_mode
        self.dmx_config = dmx_config or {}
        self.last_scene = None
        self.gemini_key = get_ultima_credential("UG_S1", "GEMINI_API_KEY")
        self.auth_token = get_ultima_credential("UG_S2") or "VOID_TOKEN"
        self.current_interval = SYNC_INTERVAL
        self.smoothed_faders = {}

        # Initialize 512-channel DMX Buffer
        self.dmx_buffer = [0] * 512
        self.ser = None
        self.artnet_universe = None

        # Register signal handlers for graceful shutdown
        signal.signal(signal.SIGINT, self.exit_gracefully)
        signal.signal(signal.SIGTERM, self.exit_gracefully)

    def exit_gracefully(self, signum, frame):
        """Handles termination signals by stopping the main loop."""
        print(f"\n[INFO] Termination signal ({signum}) received. Closing all hardware interfaces...")
        self.running = False
        
        if self.ser and self.ser.is_open:
            # Blackout on exit
            self.ser.write(bytearray([0] * 513))
            self.ser.close()
        
        # Certificate Mapping
        self.cert = (os.getenv("UG_CERT_PATH"), os.getenv("UG_CERT_KEY"))
        self.ca_bundle = os.getenv("UG_CA_BUNDLE")
        
        print("[INFO] DMX Sync Engine initialized. Awaiting system handshake...")
        if self.offline:
            print("[OFFLINE] Mode Active: Simulating local environment logic.")
        else:
            print(f"[INFO] Monitoring sync signals from {self.host_url}...")

    def setup_hardware(self):
        """Initializes Serial or Art-Net interfaces."""
        if self.dmx_mode == "serial" and serial:
            port = self.dmx_config.get("port", "COM3")
            try:
                self.ser = serial.Serial(port, baudrate=250000, stopbits=2)
                print(f"[HW] Serial DMX initialized on {port}")
            except Exception as e:
                print(f"[ERROR] Failed to open Serial Port: {e}")

        elif self.dmx_mode == "artnet" and ArtNetNode:
            target_ip = self.dmx_config.get("ip", "127.0.0.1")
            universe_id = self.dmx_config.get("universe", 0)
            # ArtNet implementation often requires an event loop; we wrap it here
            self.node = ArtNetNode(target_ip)
            self.artnet_universe = self.node.add_universe(universe_id)
            print(f"[HW] Art-Net initialized for {target_ip} (Universe {universe_id})")

    def _send_dmx(self):
        """Dispatches the buffer to the hardware."""
        if self.dmx_mode == "serial" and self.ser:
            # Start code (0) + 512 channels
            packet = bytearray([0] + self.dmx_buffer)
            self.ser.write(packet)
        elif self.dmx_mode == "artnet" and self.artnet_universe:
            # Standard pyartnet usage
            self.artnet_universe.set_data(self.dmx_buffer)

    def _val_to_dmx(self, val):
        """Converts 0.0-1.0 float to 0-255 DMX byte."""
        return int(max(0.0, min(1.0, val)) * 255)

    def apply_dmx_logic(self, state):
        """Simulates sending DMX universe updates based on aggregated system state."""
        bpm = state.get("bpm", 120)
        faders = state.get("faders", {})
        strobe = state.get("strobe_active", False)

        # Apply Exponential Moving Average (EMA) filtering (Alpha: 0.2)
        # This provides a smooth decay/attack for fader transitions
        alpha = 0.2
        input_data = {**faders, "fog": state.get("fog_density", 0)}
        for key, raw_val in input_data.items():
            prev_val = self.smoothed_faders.get(key, raw_val)
            self.smoothed_faders[key] = (alpha * raw_val) + ((1.0 - alpha) * prev_val)

        # Calculate dynamic interval: Sync at 1/4 beat resolution (60 / BPM * 0.25)
        # We use max(0.01, ...) to prevent accidental zero-division or over-taxing the CPU
        if bpm > 0:
            self.current_interval = max(0.02, (60.0 / bpm) * 0.25)

        # Monitor Telemetry
        telemetry = state.get("telemetry")
        if telemetry:
            mem = telemetry.get("memory_usage", 0)
            latency = telemetry.get("network_latency_ms", 0)
            if latency > 100 or mem > 85:
                print(f"\033[93m[WARN] System Jitter: {latency}ms Latency | {mem}% Memory Usage\033[0m")

        # Read the visual mode directly from the Android host state
        current_scene = state.get("visual_mode", 0)

        if current_scene != self.last_scene:
            scene_meta = {
                0: ("\033[96m", "LINEAR PROJECTION"),
                1: ("\033[95m", "DYNAMIC FLUIDITY"),
                2: ("\033[93m", "QUANTUM LATTICE")
            }
            color, name = scene_meta.get(current_scene, ("\033[0m", "UNKNOWN"))
            print(f"{color}[SYSTEM] Environment Transition -> {name} (Sync: {bpm} BPM)\033[0m")
            self.last_scene = current_scene

        # --- Physical DMX Mapping ---
        # Channels 1-5: Audio Faders
        self.dmx_buffer[0] = self._val_to_dmx(self.smoothed_faders.get("low", 0))
        self.dmx_buffer[1] = self._val_to_dmx(self.smoothed_faders.get("mid", 0))
        self.dmx_buffer[2] = self._val_to_dmx(self.smoothed_faders.get("high", 0))
        self.dmx_buffer[3] = self._val_to_dmx(self.smoothed_faders.get("vocal", 0))
        self.dmx_buffer[4] = self._val_to_dmx(self.smoothed_faders.get("sub", 0))
        
        # Channel 6: Strobe (Pulse intensity)
        self.dmx_buffer[5] = 255 if strobe else 0

        # Channel 7: Laser
        self.dmx_buffer[6] = 255 if state.get("laser_active", False) else 0
        
        # Channel 8: Fog
        self.dmx_buffer[7] = self._val_to_dmx(self.smoothed_faders.get("fog", 0))

        # Dispatch to Hardware
        if not self.offline:
            self._send_dmx()
        elif strobe:
            print(f"[SIM] Strobe Pulse | Buffer[0..4]: {self.dmx_buffer[:5]}")

    def _get_simulation_state(self):
        """Generates dynamic mock data for standalone testing."""
        t = time.time()
        return {
            "bpm": 120 + (int(t) % 10),  # Simulates slight BPM drift
            "faders": {
                "low": 0.5 + 0.2 * math.sin(t * 0.5),
                "mid": 0.5 + 0.2 * math.cos(t * 0.5),
                "high": random.uniform(0.4, 0.8),
                "sub": 0.3 + 0.1 * math.sin(t)
            },
            "strobe_active": (int(t) % 2 == 0),
            "laser_active": (int(t) % 15 == 0),
            "visual_mode": (int(t) // 10) % 3,
            "telemetry": {"memory_usage": 18, "network_latency_ms": 0}
        }

    def _fetch_remote_state(self):
        """Handles the network handshake with the Android Host."""
        for attempt in range(1, 4):
            try:
                headers = {"Authorization": f"Bearer {self.auth_token}"}
                cert_data = self.cert if all(self.cert) else None
                response = requests.get(
                    self.host_url, headers=headers, cert=cert_data, verify=False, timeout=1
                )
                if response.status_code == 200:
                    return response.json()
                elif attempt == 3:
                    print(f"[WARN] Bridge unreachable. Status: {response.status_code}")
            except Exception as e:
                if attempt == 3:
                    print(f"[ERROR] Sync failure: {str(e)}")
            if attempt < 3:
                time.sleep(0.1)
        return None

    def start(self):
        while self.running:
            state = None
            if self.offline:
                state = self._get_simulation_state()
            else:
                state = self._fetch_remote_state()

            if state:
                self.apply_dmx_logic(state)
            time.sleep(self.current_interval)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="DJ MIDI WATTS Sync Engine")
    parser.add_argument("--ip", default="localhost", help="IP address of the Android Host (default: localhost)")
    parser.add_argument("--port", type=int, default=8081, help="Port of the Android Host (default: 8081)")
    parser.add_argument("-o", "--offline", action="store_true", help="Skip bridge connection and run simulation logic")
    parser.add_argument("--dmx-mode", choices=["none", "serial", "artnet"], default="none")
    parser.add_argument("--serial-port", default="COM3")
    parser.add_argument("--artnet-ip", default="127.0.0.1")
    args = parser.parse_args()

    host_url = f"http://{args.ip}:{args.port}/api/state"

    # Create requirements.txt dynamically if not exists
    req_path = os.path.join(script_dir, "requirements.txt")
    with open(req_path, "w", encoding="utf-8") as f:
        f.write("requests>=2.25.1\npython-dotenv>=1.0.0\ngoogle-auth>=2.0.0\npyserial>=3.5\npyartnet>=0.8.2\ngoogle-cloud-api-keys>=0.9.0\n")
        
    dmx_cfg = {"port": args.serial_port, "ip": args.artnet_ip}
    module = SyncModule(host_url, offline=args.offline, dmx_mode=args.dmx_mode, dmx_config=dmx_cfg)
    
    try:
        if not args.offline:
            module.setup_hardware()
        module.start()
    except KeyboardInterrupt:
        print("\n[SYSTEM] Python Sync Engine stopped.")