# DJ MIDI WATTS - Documentation to Jupyter Notebook Converter
# Compiles markdown guides and python commands into a unified notebook workspace

import os
import json

# Resolve workspace paths
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT_DIR = os.path.dirname(SCRIPT_DIR)
NOTEBOOKS_DIR = os.path.join(ROOT_DIR, "notebooks")
OUTPUT_NOTEBOOK = os.path.join(NOTEBOOKS_DIR, "workspace.ipynb")

def build_notebook():
    print("[NOTEBOOK] Initializing document conversion...")
    
    # Ensure output notebooks directory exists
    if not os.path.exists(NOTEBOOKS_DIR):
        os.makedirs(NOTEBOOKS_DIR)
        
    cells = []
    
    # Welcome cell
    cells.append({
        "cell_type": "markdown",
        "metadata": {},
        "source": [
            "# DJ MIDI WATTS - Unified Command Deck & Workspace\n",
            "This notebook integrates the markdown documentation indexes and executable scripts for testing and running the sync engine.\n",
            "\n",
            "### Notebook Features:\n",
            "- Execute diagnostics directly from code blocks.\n",
            "- Interactive sync engine control and telemetry checks.\n",
            "- Consolidated technical documentation indices."
        ]
    })

    # List of documentation files to compile
    docs = [
        ("README.md", "Project Overview"),
        ("LOGIC_INDEX.md", "System Logic & Mappings"),
        ("ADMIN_COMMANDS.md", "Administrative Commands"),
        ("SECRET_INDEX.md", "Vault & Credentials Index")
    ]
    
    for filename, title in docs:
        filepath = os.path.join(ROOT_DIR, filename)
        if os.path.exists(filepath):
            print(f"[NOTEBOOK] Compiling document: {filename} ({title})...")
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
                
            # Split content into lines preserving newlines
            lines = [line + "\n" for line in content.split("\n")]
            # Strip trailing newline off the very last line to avoid double spacer
            if lines:
                lines[-1] = lines[-1].rstrip("\n")
                
            current_source = [
                f"--- \n",
                f"## {title} (`{filename}`)\n",
                "--- \n"
            ]
            
            for line in lines:
                # Break cells on headers or roughly every 50 lines to prevent cell overflow
                if line.startswith("#") and len(current_source) > 5:
                    if current_source:
                        cells.append({
                            "cell_type": "markdown",
                            "metadata": {},
                            "source": current_source
                        })
                    current_source = [line]
                else:
                    current_source.append(line)
                    
            if current_source:
                cells.append({
                    "cell_type": "markdown",
                    "metadata": {},
                    "source": current_source
                })
        else:
            print(f"[NOTEBOOK] Optional guide not found: {filename}")

    # Add code cells for executing sync diagnostics
    cells.append({
        "cell_type": "markdown",
        "metadata": {},
        "source": [
            "## Executable Toolchain Tasks\n",
            "Use the code blocks below to audit and launch the system components directly from the Jupyter runtime."
        ]
    })
    
    # Code block to run diagnostics
    cells.append({
        "cell_type": "code",
        "execution_count": None,
        "metadata": {},
        "outputs": [],
        "source": [
            "# Run the System Diagnostic Audit Utility (Powershell/Terminal)\n",
            "# This performs the environment checks for dependencies and credentials\n",
            "import os\n",
            "print('Running diagnostics...\\n')\n",
            "os.system('powershell -ExecutionPolicy Bypass -File ../foo.ps1')"
        ]
    })

    # Code block to run grid auditor
    cells.append({
        "cell_type": "code",
        "execution_count": None,
        "metadata": {},
        "outputs": [],
        "source": [
            "# Run the Python Grid Auditor\n",
            "# Checks for neural fragmentation and audits the command deck integrity\n",
            "import os\n",
            "print('Auditing MIDI and hardware mappings...\\n')\n",
            "os.system('python ../scripts/grid_auditor.py')"
        ]
    })

    # Assemble notebook structure
    notebook = {
        "cells": cells,
        "metadata": {
            "kernelspec": {
                "display_name": "Python 3",
                "language": "python",
                "name": "python3"
            },
            "language_info": {
                "name": "python"
            }
        },
        "nbformat": 4,
        "nbformat_minor": 2
    }

    # Write output
    with open(OUTPUT_NOTEBOOK, "w", encoding="utf-8") as f:
        json.dump(notebook, f, indent=1)
        
    print(f"[SUCCESS] Jupyter Notebook compiled successfully at: {OUTPUT_NOTEBOOK}")

if __name__ == "__main__":
    build_notebook()
