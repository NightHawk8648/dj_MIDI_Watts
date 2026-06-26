/**
 * DJ MIDI WATTS - Ultima-Grid Troubleshooting Index
 * Reference for Status Codes
 */
const UG_ERROR_INDEX = {
    "UG_100": "PORTAL_OFFLINE: Localhost server not responding.",
    "UG_200": "GRID_SYNC: All systems operational.",
    "UG_301": "S1_VOID: Gemini AI Orchestration Key missing or invalid.",
    "UG_302": "MIDI_DETACHED: Physical hardware or Virtual MIDI link not found.",
    "UG_303": "SEC_COMPROMISED: Security integrity check failed.",
    "UG_404": "PLAYER_ERR: Audio/Video engine initialization failed."
};

if (typeof module !== 'undefined') module.exports = UG_ERROR_INDEX;