/**
 * DJ MIDI WATTS - Background Telemetry Sync Service
 */
const FALLBACK_SERVERS = [
    'https://djmidiwatts-live.app',
    'http://radeon3:8080',
    'http://localhost:8081', // Remote SSH tunnel default
    'http://100.100.100.100:8080', // Replace with your Tailscale IP
    'http://localhost:8080'
];

let activeHostUrl = null;
let isDiscovering = false;

async function discoverActiveServer() {
    if (isDiscovering) return activeHostUrl;
    isDiscovering = true;
    
    for (const url of FALLBACK_SERVERS) {
        try {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 1500);
            
            // We use standard fetch here just to ping the server quickly without auth prompt blocks
            const response = await fetch(`${url}/api/state`, { signal: controller.signal });
            clearTimeout(timeoutId);
            
            if (response.ok || response.status === 401 || response.status === 403) {
                console.log(`[BG] Active server discovered: ${url}`);
                activeHostUrl = url;
                chrome.storage.local.set({ activeHostUrl: url });
                isDiscovering = false;
                return url;
            }
        } catch (e) {
            console.log(`[BG] Server unreachable: ${url}`);
        }
    }
    
    console.warn("[BG] All servers unreachable.");
    activeHostUrl = FALLBACK_SERVERS[0]; // fallback to default
    chrome.storage.local.set({ activeHostUrl: activeHostUrl });
    isDiscovering = false;
    return activeHostUrl;
}

// Initial discovery on load
discoverActiveServer();
setInterval(discoverActiveServer, 60000); // Re-check every minute

// Authenticated fetch wrapper for Google Cloud API
async function authFetch(url, options = {}) {
    try {
        const { token } = await chrome.identity.getAuthToken({ interactive: true });
        if (!token) throw new Error("Token fetch failed");
        
        const headers = options.headers || {};
        headers['Authorization'] = `Bearer ${token}`;
        
        return await fetch(url, { ...options, headers });
    } catch (error) {
        console.error("[Auth] Token fetch failed", error);
        throw error;
    }
}



// Local cache to prevent redundant storage writes
let lastSerializedState = "";

chrome.runtime.onInstalled.addListener(() => {
    console.log("[BG] Neural Link active. Initializing background sync...");
});


async function fetchGridTelemetry() {
    try {
        if (!activeHostUrl) await discoverActiveServer();
        // Target the new root state endpoint for a full system snapshot
        const response = await authFetch(`${activeHostUrl}/api/state`);
        if (response.ok) {
            const state = await response.json();
            const serialized = JSON.stringify(state);

            // Optimization: Only write to storage if the state has actually changed
            if (serialized !== lastSerializedState) {
                chrome.storage.local.set({ lastKnownState: state, lastSync: Date.now() });
                lastSerializedState = serialized;
                console.log("[BG] Grid state updated in local storage.");
            }
        }
    } catch (e) {
        // Grid is unreachable or dark; awaiting resonance
        lastSerializedState = ""; 
        console.warn("[BG] Cloud API unreachable.", e);
    }
}

// Listen for FX triggers from the popup and broadcast them to active tabs
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.action === "triggerVisualFX") {
        // Optimization: Filter for http/https tabs only to reduce IPC overhead
        chrome.tabs.query({ url: ["http://*/*", "https://*/*"] }).then((tabs) => {
            tabs.forEach(tab => {
                chrome.tabs.sendMessage(tab.id, {
                    action: "triggerVisualFX",
                    fx: message.fx,
                    value: message.value,
                    color: message.color
                }).catch(() => {
                    // Silent catch for tabs where content script isn't injected
                });
            });
        }).catch(err => console.error(err));
    } else if (message.action === "spotify_metadata") {
        // Relay Spotify metadata to the Android API via Cloud
        const url = new URL('/api/spotify/sync', activeHostUrl || FALLBACK_SERVERS[0]);
        url.searchParams.append('title', message.title);
        url.searchParams.append('artist', message.artist);

        authFetch(url.toString())
            .catch(() => { 
                console.warn("[BG] Spotify sync failed - Cloud API unreachable.");
            });
    } else if (message.status === "connected") {
        chrome.action.setBadgeText({ text: "ON" });
        chrome.action.setBadgeBackgroundColor({ color: "#00FFCC" });
    } else if (message.status === "disconnected") {
        chrome.action.setBadgeText({ text: "" });
    } else if (message.action === "forceDiscovery") {
        discoverActiveServer().then(url => sendResponse({ url }));
        return true; // Keep channel open for async response
    }
    return true; 
});