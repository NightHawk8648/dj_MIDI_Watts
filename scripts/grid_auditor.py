import asyncio
import sys
from google.antigravity import Agent, LocalAgentConfig
from google.antigravity.hooks import policy
import os

# Force standard streams to use UTF-8 to prevent UnicodeEncodeError on Windows
if sys.platform.startswith('win'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except AttributeError:
        pass


async def main():
    # Move: Initialize the Auditor Agent (Tier 4.5 Debugger logic)
    instructions = """
    You are the 'Neural Trace' Debugger (Tier 4.5).
    Your task is to audit the DJ-MIDI-WATTS project.
    1. Compare LOGIC_INDEX.md against the current repository state.
    2. Identify 'Neural Fragmentation' (unresolved code or path references).
    3. Ensure 'Path Neutrality' is maintained for multi-OS compliance.
    4. Specifically verify the existence of files listed in Section 16 (Master File Layer) such as CommanderViewModel.kt and WebUIHost.kt.
    5. Cross-reference the MIDI CC mappings in Section 2 with the logic in sync_engine.py.
    """
    
    # Dynamically determine the skills_paths relative to the script's location
    script_dir = os.path.dirname(__file__)
    project_root = os.path.abspath(os.path.join(script_dir, os.pardir, os.pardir))
    antigravity_sdk_path = os.path.join(project_root, "antigravity-sdk-python", "skills", "google-antigravity-sdk")

    config = LocalAgentConfig(
        system_instructions=instructions,
        # Allow the agent to explore the project structure to find fragments
        policies=[
            policy.allow("view_file"), 
            policy.allow("list_directory"),
            policy.deny_all()
        ],
        # Scoped to the project root
        skills_paths=[antigravity_sdk_path]
    )

    async with Agent(config) as auditor:
        print("🚀 Tier 4.5 Debugger: Initiating Neural Trace...")
        response = await auditor.chat("Audit LOGIC_INDEX.md for consistency with README.md and ADMIN_COMMANDS.md.")
        print(f"Audit Result: {await response.text()}")

if __name__ == "__main__":
    asyncio.run(main())