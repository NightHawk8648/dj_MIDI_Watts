import os
import sys
import subprocess
import shutil

# Resolve absolute paths relative to this script
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
BRIDGE_DIR = os.path.dirname(SCRIPT_DIR)
DESKTOP_DIR = os.path.dirname(BRIDGE_DIR)
VENV_DIR = os.path.join(ROOT_DIR, ".venv")
ROOT_DIR = os.path.dirname(DESKTOP_DIR)
REQUIREMENTS_PATH = os.path.join(ROOT_DIR, "scripts", "requirements.txt")

# Determine OS execution settings
IS_WINDOWS = sys.platform.startswith("win")
VENV_BIN_SUBDIR = "Scripts" if IS_WINDOWS else "bin"
PYTHON_EXE_NAME = "python.exe" if IS_WINDOWS else "python"
PYTHONW_EXE_NAME = "pythonw.exe" if IS_WINDOWS else "pythonw"

VENV_PYTHON = os.path.join(VENV_DIR, VENV_BIN_SUBDIR, PYTHON_EXE_NAME)
VENV_PYTHONW = os.path.join(VENV_DIR, VENV_BIN_SUBDIR, PYTHONW_EXE_NAME)

print(f"[SETUP] Root directory resolved to: {ROOT_DIR}")
print(f"[SETUP] Target Venv directory: {VENV_DIR}")
print(f"[SETUP] Requirements file: {REQUIREMENTS_PATH}")

def check_command(cmd):
    """Check if a command exists on the system path."""
    return shutil.which(cmd) is not None

def run_cmd(args, cwd=None):
    """Execute a system command and print output on failure."""
    try:
        result = subprocess.run(args, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, cwd=cwd)
        return True, result.stdout
    except subprocess.CalledProcessError as e:
        print(f"[ERROR] Command failed: {' '.join(args)}")
        print(f"Error output:\n{e.stderr}")
        return False, e.stderr

def setup_virtualenv():
    """Create the virtual environment using uv (if available) or standard venv."""
    print("[SETUP] Initializing virtual environment...")
    
    # Try to find a standard python 3.14 or default to the running interpreter version
    python_ver = "3.14.5"
    
    if check_command("uv"):
        print("[SETUP] Found 'uv' installer cache. Creating environment with uv...")
        # uv venv --python 3.14.5 --seed --clear <path>
        cmd = ["uv", "venv", "--python", python_ver, "--seed", "--clear", VENV_DIR]
        success, out = run_cmd(cmd)
        if not success:
            print("[SETUP] uv venv creation failed. Falling back to standard venv...")
            cmd = [sys.executable, "-m", "venv", "--clear", VENV_DIR]
            success, out = run_cmd(cmd)
    else:
        print("[SETUP] 'uv' not found. Creating environment with standard venv...")
        cmd = [sys.executable, "-m", "venv", "--clear", VENV_DIR]
        success, out = run_cmd(cmd)
    
    return success

def install_requirements():
    """Install dependencies into the virtual environment using uv or pip."""
    if not os.path.exists(VENV_PYTHON):
        print(f"[ERROR] Virtual environment python not found at {VENV_PYTHON}")
        return False

    print("[SETUP] Installing package requirements...")
    
    if not os.path.exists(REQUIREMENTS_PATH):
        print(f"[WARNING] Requirements file not found at {REQUIREMENTS_PATH}. Creating standard dependencies...")
        deps = ["requests", "python-dotenv", "google-auth", "pyserial", "pyartnet", "google-cloud-api-keys"]
        if check_command("uv"):
            cmd = ["uv", "pip", "install", "--python", VENV_PYTHON] + deps
        else:
            cmd = [VENV_PYTHON, "-m", "pip", "install"] + deps
        success, out = run_cmd(cmd)
    else:
        if check_command("uv"):
            # uv pip install -r requirements.txt --python <venv_python>
            cmd = ["uv", "pip", "install", "-r", REQUIREMENTS_PATH, "--python", VENV_PYTHON]
        else:
            # First ensure pip is upgraded in the seeded venv
            run_cmd([VENV_PYTHON, "-m", "pip", "install", "--upgrade", "pip"])
            cmd = [VENV_PYTHON, "-m", "pip", "install", "-r", REQUIREMENTS_PATH]
        success, out = run_cmd(cmd)
    
    return success

def verify_bridge():
    """Verify that python and pythonw executables function correctly in the environment."""
    print("[SETUP] Running sanity check verification...")
    if not os.path.exists(VENV_PYTHON):
        return False
    
    # Test execution and package import
    test_code = "import sys, requests, dotenv, google.auth; print('Venv Check: Python v' + sys.version.split()[0] + ' OK')"
    cmd = [VENV_PYTHON, "-c", test_code]
    success, out = run_cmd(cmd)
    
    if success:
        print(f"[VERIFY] {out.strip()}")
        # Check pythonw.exe exists
        if IS_WINDOWS:
            if os.path.exists(VENV_PYTHONW):
                print("[VERIFY] pythonw.exe verified successfully.")
            else:
                print("[WARNING] pythonw.exe is missing from Scripts folder.")
    else:
        print("[ERROR] Venv package imports failed verification check.")
        
    return success

if __name__ == "__main__":
    if setup_virtualenv():
        if install_requirements():
            if verify_bridge():
                print("[SUCCESS] Cross-platform Python Venv Bridge setup is complete and healthy.")
                sys.exit(0)
    print("[FAIL] Setup failed during environment provisioning.")
    sys.exit(1)
