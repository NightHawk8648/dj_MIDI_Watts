import os
import sys
import json
import argparse
import subprocess
import shutil
from pathlib import Path

# Force standard streams to use UTF-8 to prevent UnicodeEncodeError on Windows
if sys.platform.startswith('win'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except AttributeError:
        pass


class WorkspaceEngine:
    """
    The Ultima-Grid Global Engine: Orchestrating the interface between 
    Android, Web UI, and IDE contexts.
    """
    def __init__(self, workspace_root=None):
        # Resolve the root: provided path or current working directory
        self.root = Path(workspace_root or os.getcwd()).resolve()
        self.app_path = self.root / "app"
        self.scripts_path = self.root / "scripts"
        
        print(f"🚀 [ULTIMA-GRID] Global Engine initialized at: {self.root}")

    def _validate_environment(self):
        """
        Validates that critical toolchains (ADB, Gradle, Java, IAR, CS-Script, PS) are accessible.
        """
        print("🔍 [ENGINE] Validating system environment...")
        
        tools = ["adb", "gradle", "java", "iarbuild", "cscs", "powershell"]
        for tool in tools:
            path = shutil.which(tool)
            
            # Heuristic for IAR if not in PATH
            if not path and tool == "iarbuild" and os.name == "nt":
                iar_search = list(Path("C:/Program Files/IAR Systems").glob("**/iarbuild.exe"))
                if iar_search:
                    path = str(iar_search[0])

            if path:
                print(f"   ✅ {tool} found: {path}")
                if tool == "java":
                    try:
                        output = subprocess.check_output([tool, "-version"], stderr=subprocess.STDOUT, text=True)
                        version_line = output.splitlines()[0]
                        print(f"   ℹ️  {version_line}")
                    except Exception:
                        print("   ⚠️  Unable to retrieve Java version details.")
                
                if tool == "cscs":
                    try:
                        output = subprocess.check_output([tool, "-version"], stderr=subprocess.STDOUT, text=True)
                        print(f"   ℹ️  CS-Script Version: {output.strip()}")
                    except Exception:
                        print("   ⚠️  Unable to retrieve CS-Script version details.")

                if tool == "iarbuild":
                    try:
                        # iarbuild displays its version banner when executed without arguments
                        res = subprocess.run([path], capture_output=True, text=True)
                        output = res.stdout if res.stdout else res.stderr
                        if output:
                            print(f"   ℹ️  {output.splitlines()[0].strip()}")
                    except Exception:
                        print("   ⚠️  Unable to retrieve IAR version details.")

                if tool == "powershell":
                    try:
                        policy = subprocess.check_output([tool, "-Command", "Get-ExecutionPolicy"], text=True).strip()
                        print(f"   ℹ️  PowerShell Execution Policy: {policy}")
                        if policy in ["Restricted", "AllSigned"]:
                            print("   ⚠️  Warning: Policy may block unsigned project scripts.")
                    except Exception:
                        pass
            else:
                if tool == "gradle":
                    wrapper = "gradlew.bat" if os.name == "nt" else "gradlew"
                    if (self.root / wrapper).exists():
                        print(f"   ℹ️  System {tool} missing, using local {wrapper}")
                        continue
                print(f"   ⚠️  {tool} not found in system PATH.")

        # 1. Project File Integrity (Cross-reference with foo.ps1 Tier 2.2)
        iar_project = self.root / "firmware" / "ultimate_grid_core.ewp"
        if iar_project.exists():
            print(f"   ✅ IAR Project File detected: {iar_project}")
        else:
            print(f"   ⚠️  IAR Project missing: {iar_project} (Firmware builds will fail)")

        # 2. Secrets Validation (Vault & Translator Layer)
        print("🔐 [ENGINE] Auditing Secrets and Credentials...")
        required_secrets = ["local.properties", ".env"]
        for secret in required_secrets:
            if (self.root / secret).exists():
                print(f"   ✅ Secret container found: {secret}")
            else:
                print(f"   ❌ Missing: {secret}. Run 'provision' via cli_integration.ps1.")

        # 3. Cloud Sync Audit (Optional Phase 4)
        print("☁️ [ENGINE] Auditing Personal Cloud configuration...")
        local_props = self.root / "local.properties"
        if local_props.exists():
            with open(local_props, "r", encoding="utf-8") as f:
                props = f.read()
                if "cloud.server.ip" in props:
                    print("   ✅ Personal Cloud configuration detected.")
                else:
                    print("   ℹ️  Personal Cloud: Local environment active. (Self-hosting not configured)")
        else:
            print("   ℹ️  Personal Cloud: local.properties missing. Defaulting to local environment.")

    def _inject_ide_rules(self):
        """
        Blends DJ MIDI WATTS workspace requirements with Gradle backend logic.
        Tracks extension manifests and build contexts to ensure IDE parity.
        """
        print("🔧 [ENGINE] Injecting IDE rules and workspace context...")

        # 1. Track Extension Manifest (Chrome Extension Context)
        ext_manifest = self.root / "dj-midi-watts-extension" / "manifest.json"
        ext_status = "MISSING"
        if ext_manifest.exists():
            try:
                with open(ext_manifest, "r", encoding="utf-8") as f:
                    manifest_data = json.load(f)
                    ext_status = f"ACTIVE (v{manifest_data.get('version', '0.0.1')})"
            except Exception:
                ext_status = "CORRUPT"
        
        # 2. Extract Gradle Backend Logic (Build Context)
        # We peek into build.gradle.kts to align IDE settings with the current SDK target
        gradle_config = self.app_path / "build.gradle.kts"
        target_sdk = "unknown"
        if gradle_config.exists():
            with open(gradle_config, "r", encoding="utf-8") as f:
                content = f.read()
                if "targetSdk = " in content:
                    target_sdk = content.split("targetSdk = ")[1].split("\n")[0].strip()

        # 3. Blending into .vscode/settings.json
        vscode_dir = self.root / ".vscode"
        vscode_dir.mkdir(exist_ok=True)
        settings_file = vscode_dir / "settings.json"
        
        settings = {}
        if settings_file.exists():
            try:
                with open(settings_file, "r", encoding="utf-8") as f:
                    settings = json.load(f)
            except Exception:
                pass
        
        settings.update({
            "dj-midi-watts.workspaceRoot": str(self.root),
            "dj-midi-watts.extensionStatus": ext_status,
            "dj-midi-watts.targetSdk": target_sdk,
            "python.analysis.extraPaths": [
                str(self.app_path / "src" / "main" / "java" / "com" / "example" / "ui"),
                str(self.scripts_path)
            ],
            "files.exclude": settings.get("files.exclude") or {
                "**/.git": True,
                "**/.gradle": True,
                "**/build": True,
                "**/node_modules": True
            },
            "editor.formatOnSave": settings.get("editor.formatOnSave", True),
            "java.import.gradle.java.home": "C:\\Program Files\\Java\\jdk-17",
            "java.import.gradle.wrapper.enabled": True,
            "java.import.gradle.version": "8.11.1"
        })

        with open(settings_file, "w", encoding="utf-8") as f:
            json.dump(settings, f, indent=4)

        # 4. Generate/Update .vscode/launch.json
        launch_config = {
            "version": "0.2.0",
            "configurations": [
                {
                    "name": "🚀 RUN: Sync Engine (Python)",
                    "type": "debugpy",
                    "request": "launch",
                    "program": "${workspaceFolder}/scripts/sync_engine.py",
                    "console": "integratedTerminal",
                    "args": ["--ip", "auto"]
                },
                {
                    "name": "🏥 DIAG: System Health (-RunTests)",
                    "type": "powershell",
                    "request": "launch",
                    "script": "${workspaceFolder}/foo.ps1",
                    "args": ["-RunTests"],
                    "cwd": "${workspaceFolder}"
                },
                {
                    "name": "♻️  ADMIN: Master Reset (Scorch Grid)",
                    "type": "powershell",
                    "request": "launch",
                    "script": "${workspaceFolder}/foo.ps1",
                    "args": ["-MasterReset"],
                    "cwd": "${workspaceFolder}"
                },
                {
                    "name": "🧹 SECURITY: Cleanup Vault",
                    "type": "powershell",
                    "request": "launch",
                    "script": "${workspaceFolder}/foo.ps1",
                    "args": ["-CleanupVault"],
                    "cwd": "${workspaceFolder}"
                },
                {
                    "name": "🏗️ BUILD: Ultimate Assembly (Gradle)",
                    "type": "java",
                    "request": "launch",
                    "mainClass": "",
                    "projectName": "app",
                    "preLaunchTask": "gradle: ultimateGridAssembly"
                },
                {
                    "name": "🔍 AUDIT: Grid Auditor (Python Agent)",
                    "type": "debugpy",
                    "request": "launch",
                    "program": "${workspaceFolder}/scripts/grid_auditor.py",
                    "console": "integratedTerminal"
                },
                {
                    "name": "📜 SCRIPT: CS-Script Executor",
                    "type": "coreclr",
                    "request": "launch",
                    "program": "cscs",
                    "args": ["${file}"],
                    "cwd": "${workspaceFolder}",
                    "stopAtEntry": False
                }
            ]
        }

        launch_file = vscode_dir / "launch.json"
        with open(launch_file, "w", encoding="utf-8") as f:
            json.dump(launch_config, f, indent=4)

        print(f"   ✅ IDE Context Updated: SDK {target_sdk} | Extension: {ext_status}")
        print("   🚀 launch.json synchronized with master task list.")

    def run_health_check(self):
        """
        Triggers health audits for SDK parity and neural consistency.
        """
        print("🏥 [ENGINE] Running health check...")
        
        # 1. Extract targetSdk from Gradle for validation
        target_sdk = "unknown"
        gradle_config = self.app_path / "build.gradle.kts"
        if gradle_config.exists():
            with open(gradle_config, "r", encoding="utf-8") as f:
                content = f.read()
                if "targetSdk = " in content:
                    target_sdk = content.split("targetSdk = ")[1].split("\n")[0].strip()

        # 2. Verify Android SDK platform installation parity
        if target_sdk != "unknown":
            sdk_path = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
            
            # Windows Fallback
            if not sdk_path and os.name == "nt":
                local_app_data = os.environ.get("LOCALAPPDATA")
                if local_app_data:
                    potential_path = Path(local_app_data) / "Android" / "Sdk"
                    if potential_path.exists():
                        sdk_path = str(potential_path)

            if sdk_path:
                platform_dir = Path(sdk_path) / "platforms" / f"android-{target_sdk}"
                if platform_dir.exists():
                    print(f"   ✅ Android SDK: Platform {target_sdk} is installed.")
                else:
                    print(f"   ❌ Android SDK: Platform {target_sdk} is MISSING from {sdk_path}.")
                    print(f"      Run: sdkmanager \"platforms;android-{target_sdk}\"")
            else:
                print("   ⚠️  Android SDK: Path not found. Set ANDROID_HOME environment variable.")

        # 3. Trigger the Python Grid Auditor
        auditor = self.scripts_path / "grid_auditor.py"
        if auditor.exists():
            print("   🔍 Launching Grid Auditor...")
            subprocess.run([sys.executable, str(auditor)], cwd=self.root)

    def setup_personal_cloud(self):
        """
        Interactive setup for the optional Personal Cloud sync (Phase 4).
        """
        print("\n☁️ [SETUP] Optional: Personal Cloud Configuration")
        choice = input("Would you like to configure a Personal Cloud (Tailscale + Nextcloud)? (y/N): ").lower()
        if choice != 'y':
            print("   ℹ️  Skipping cloud setup. Continuing with local environment.")
            return

        ip = input("   -> Enter Server Tailscale IP (100.x.x.x): ")
        path = input(f"   -> Enter Remote API Path: ")
        wifi = input("   -> Enter Debug WiFi Target IP (Optional): ")

        local_props = self.root / "local.properties"
        lines = []
        if local_props.exists():
            with open(local_props, "r", encoding="utf-8") as f:
                lines = f.readlines()

        # Filter out existing cloud properties to ensure clean integration
        lines = [l for l in lines if not l.startswith("cloud.")]

        with open(local_props, "w", encoding="utf-8") as f:
            f.writelines(lines)
            f.write(f"\n# Remote Cloud Configuration for Runtime Operations\n")
            f.write(f"cloud.server.ip={ip}\n")
            f.write(f"cloud.server.api.path={path}\n")
            f.write(f"cloud.storage.secure_mode=true\n")
            if wifi:
                f.write(f"cloud.debug.wifi_target={wifi}\n")

        print(f"   ✅ Configuration written to local.properties. Integration successful.")

def main():
    parser = argparse.ArgumentParser(description="DJ MIDI WATTS Global Workspace Engine")
    parser.add_argument("path", nargs="?", default=os.getcwd(), help="Target workspace path")
    parser.add_argument("--setup-cloud", action="store_true", help="Interactively configure personal cloud sync")
    args = parser.parse_args()

    engine = WorkspaceEngine(args.path)
    if args.setup_cloud:
        engine.setup_personal_cloud()
        return

    engine._validate_environment()
    engine._inject_ide_rules()
    engine.run_health_check()

if __name__ == "__main__":
    main()