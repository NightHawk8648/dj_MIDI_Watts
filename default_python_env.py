import os
import sys
import subprocess
from pathlib import Path

# This is the default path to the Python executable for this project
# It points to the virtual environment's python instance.
PROJECT_ROOT = Path(__file__).resolve().parent
DEFAULT_PYTHON_PATH = str(PROJECT_ROOT / ".venv" / "Scripts" / "python.exe")

def get_python_path() -> str:
    """
    Returns the path to the default Python executable.
    If the script is already running within the virtual environment, 
    it returns the currently executing python instance.
    """
    # Check if we are currently running in a virtual environment
    if sys.prefix != sys.base_prefix:
        return sys.executable
    return DEFAULT_PYTHON_PATH

def run_script_with_default_python(script_path: str, *args, **kwargs):
    """
    Bridge function for any other file that calls for it.
    Executes a given python script using the project's default python executable.
    
    Args:
        script_path (str): The path to the python script to run.
        *args: Additional command line arguments to pass to the script.
        **kwargs: Additional keyword arguments to pass to subprocess.run.
        
    Returns:
        subprocess.CompletedProcess: The result of the subprocess execution.
    """
    python_exe = get_python_path()
    cmd = [python_exe, script_path, *args]
    
    # Run the command and bridge the input/output
    return subprocess.run(cmd, check=True, text=True, **kwargs)

if __name__ == "__main__":
    # If run directly, simply print the default python path
    print(f"Default Python Path: {get_python_path()}")
