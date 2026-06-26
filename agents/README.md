# DJ MIDI WATTS – Google Cloud Agent Management Hub

This directory is the dedicated location for configuring, assembling, debugging, and building Google Cloud AI Agents (e.g., Vertex AI Agents / Dialogflow CX) utilized across all 4 platforms (Android, Web UI, Chrome Extension, and Desktop Launcher).

## Directory Structure

```
agents/
├── README.md             # This instruction file
├── agent_manager.py      # Core provisioning, validation, and secure masking utility
├── build_agents.ps1      # Local CLI builder (supports: assemble, debug, build)
└── <agent_name>/         # (Future) Subfolders containing local agent specifications (intents, flows, etc.)
```

## CLI Usage

### Setup & Secure Provisioning
Run the interactive manager to verify environment credentials, prompt for permissions, and automatically cache secrets securely in your OS Vault under a masked representation:
```powershell
python agents/agent_manager.py
# Or via npm
npm run agent:setup
```

### Agent Operations
Utilize the builder script to assemble, dry-run, or deploy agents:
```powershell
# Assemble agent dependencies and assets
.\agents\build_agents.ps1 assemble

# Debug and dry-run agent verification tests
.\agents\build_agents.ps1 debug

# Build and deploy agent configs to Google Cloud Console
.\agents\build_agents.ps1 build
```
