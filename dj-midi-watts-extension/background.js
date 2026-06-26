/**
 * DJ MIDI WATTS - Background Telemetry Sync Service
 */

const hostUrl = 'http://localhost:8080';
const wsUrl = 'ws://localhost:8080/ws';
const SYNC_ALARM_NAME = 'grid-telemetry-sync';

// Local cache to prevent redundant storage writes
let lastSerializedState = "";
let socket = null;
let reconnectTimeout = 1000;
let currentReconnectDelay = 1000;
const MIN_RECONNECT_DELAY = 1000;
const MAX_RECONNECT_DELAY = 30000;

chrome.runtime.onInstalled.addListener(() => {
    console.log("[BG] Neural Link active. Initializing background sync...");
    // Setup watchdog alarm to ensure socket health every minute
    chrome.alarms.create(SYNC_ALARM_NAME, { periodInMinutes: 0.5 });
    initWebSocket();
});

chrome.alarms.onAlarm.addListener((alarm) => {
    if (alarm.name === SYNC_ALARM_NAME) {
        if (!socket || socket.readyState !== WebSocket.OPEN) {
            console.log("[BG] Watchdog: Socket dormant. Re-initializing...");
            initWebSocket();
        }
    }
});

function initWebSocket() {
    if (reconnectTimeout) {
        clearTimeout(reconnectTimeout);
        reconnectTimeout = null;
    }

    if (socket) {
        socket.close();
    }

    socket = new WebSocket(wsUrl);

    socket.onopen = () => {
        console.log("[BG] Real-time Grid Link established.");
        chrome.action.setBadgeText({ text: "LIVE" });
        chrome.action.setBadgeBackgroundColor({ color: "#00FFCC" });
        currentReconnectDelay = MIN_RECONNECT_DELAY; // Reset backoff on success
    };

    socket.onmessage = (event) => {
        try {
            const state = JSON.parse(event.data);
            const serialized = JSON.stringify(state);

            if (serialized !== lastSerializedState) {
                chrome.storage.local.set({ lastKnownState: state, lastSync: Date.now() });
                lastSerializedState = serialized;

                // Notify popup immediately if it is open
                chrome.runtime.sendMessage({
                    action: "updateUI",
                    state: state
                }).catch(() => {
                    // Ignore errors when popup is closed
                });
            }
        } catch (e) {
            console.error("[BG] Failed to parse Grid telemetry:", e);
        }
    };

    socket.onclose = () => {
        console.warn("[BG] Grid Link severed. Attempting recovery...");
        chrome.action.setBadgeText({ text: "!!" });
        chrome.action.setBadgeBackgroundColor({ color: "#FF3366" });

        // Schedule reconnection with exponential backoff
        reconnectTimeout = setTimeout(() => {
            console.log(`[BG] Reconnection attempt triggered after ${currentReconnectDelay}ms`);
            initWebSocket();
        }, currentReconnectDelay);
        currentReconnectDelay = Math.min(currentReconnectDelay * 2, MAX_RECONNECT_DELAY);
    };

    socket.onerror = (err) => {
        console.error("[BG] WebSocket Error:", err);
        socket.close();
    };
}

async function fetchGridTelemetry() {
    try {
        // Target the new root state endpoint for a full system snapshot
        const response = await fetch(`${hostUrl}/api/state`);
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
    }
}

// Listen for FX triggers from the popup and broadcast them to active tabs
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.action === "triggerVisualFX") {
        // Optimization: Filter for http/https tabs only to reduce IPC overhead
        chrome.tabs.query({ url: ["http://*/*", "https://*/*"] }, (tabs) => {
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
        });
    } else if (message.action === "spotify_metadata") {
        // Relay Spotify metadata to the Android API
        fetch(`${hostUrl}/api/spotify/sync?title=${encodeURIComponent(message.title)}&artist=${encodeURIComponent(message.artist)}`)
            .catch(() => { /* App server might be offline */ });
    } else if (message.status === "connected") {
        chrome.action.setBadgeText({ text: "ON" });
        chrome.action.setBadgeBackgroundColor({ color: "#00FFCC" });
    } else if (message.status === "disconnected") {
        chrome.action.setBadgeText({ text: "" });
    }
    return true; 
});