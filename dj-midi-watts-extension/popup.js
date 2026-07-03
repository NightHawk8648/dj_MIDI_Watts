// DJ MIDI WATTS Browser Extension Core Sync Model

let hostUrl = 'https://djmidiwatts-live.app'; // Gets updated dynamically

// Retrieve the active server discovered by background.js
chrome.storage.local.get(['activeHostUrl']).then((res) => {
    if (res.activeHostUrl) {
        hostUrl = res.activeHostUrl;
    } else {
        // If nothing is cached, ask background to discover and return it
        chrome.runtime.sendMessage({ action: "forceDiscovery" }).then((response) => {
            if (response && response.url) {
                hostUrl = response.url;
            }
        }).catch(err => console.warn(err));
    }
}).catch(err => console.error(err));

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


// DOM elements
const connStatus = document.getElementById('connection-status');
let currentThemeColor = '#00FFCC';
let lastStatus = null;
const playBtn = document.getElementById('btn-playpause');
const strobeBtn = document.getElementById('btn-strobe');
const fogBtn = document.getElementById('btn-fog');
const laserBtn = document.getElementById('btn-laser');
const spotifyBtn = document.getElementById('btn-spotify');
const trackLabel = document.getElementById('playing-track');

const faders = {
    faderSub: document.getElementById('faderSub'),
    faderLow: document.getElementById('faderLow'),
    faderMid: document.getElementById('faderMid'),
    faderVocal: document.getElementById('faderVocal'),
    faderHigh: document.getElementById('faderHigh'),
    faderFog: document.getElementById('faderFog')
};

const fValLabels = {
    faderSub: document.getElementById('val-faderSub'),
    faderLow: document.getElementById('val-faderLow'),
    faderMid: document.getElementById('val-faderMid'),
    faderVocal: document.getElementById('val-faderVocal'),
    faderHigh: document.getElementById('val-faderHigh'),
    faderFog: document.getElementById('val-faderFog')
};

// Update fader endpoint call
function transmitControl(param, value) {
    const url = new URL('/api/control', hostUrl);
    url.searchParams.append('param', param);
    url.searchParams.append('value', value);

    authFetch(url.toString())
        .then(res => res.json())
        .then(data => {
            console.log(`Synced control: ${param} -> ${value}`, data);
        })
        .catch(err => console.error('Control dispatch error:', err));
}

function transmitTrigger(fx, value = null) {
    // Detect current sub-bass level to pass to visual engine for thump effects
    const subLevel = faders.faderSub ? parseFloat(faders.faderSub.value) : 0;
    const visualValue = (fx === 'strobe') ? subLevel : value;

    const url = new URL('/api/trigger', hostUrl);
    url.searchParams.append('fx', fx);
    if (value !== null) {
        url.searchParams.append('value', value);
    }

    authFetch(url.toString())
        .then(res => res.json())
        .then(data => {
            console.log(`Triggered: ${fx}`, data);
            fetchState(); 
        })
        .catch(err => console.error('Trigger dispatch error:', err));

    // ALSO: Notify our content script framework immediately for visual responsiveness
    chrome.runtime.sendMessage({
        action: "triggerVisualFX",
        fx: fx,
        value: visualValue,
        color: currentThemeColor
    }).catch(err => {
        console.warn("Could not send message to content script:", err);
    });
}

// Polling and sync
function renderGridState(data) {
    // Send connection status to background script
    const status = data.isMidiHardwareConnected ? "connected" : "disconnected";
    if (lastStatus !== status && chrome.runtime?.id) {
        chrome.runtime.sendMessage({ status }).catch(() => {});
        lastStatus = status;
    }

    // Determine Grid Health
    const isAiActive = data.ai_active !== false;
    const isMidiActive = data.isMidiHardwareConnected !== false;
    
    if (isAiActive && isMidiActive) {
        connStatus.innerText = 'ONLINE';
        connStatus.className = 'status status-online';
    } else {
        let errorCode = !isAiActive ? 'UG_301' : 'UG_302';
        connStatus.innerText = `⚠️ ${errorCode}`;
        connStatus.className = 'status status-partial';
    }

    currentThemeColor = data.theme_color || '#00FFCC';
    document.querySelector('.title').style.color = currentThemeColor;

    if (trackLabel) {
        trackLabel.innerText = data.current_track || 'NO ACTIVE TRACK';
    }

    // Sync faders values from app state if user is not actively dragging
    Object.keys(faders).forEach(key => {
        const remoteFaders = data.faders || {};
        const apiKey = key.replace('fader', '').toLowerCase();
        if (document.activeElement !== faders[key] && remoteFaders[apiKey] !== undefined) {
            faders[key].value = remoteFaders[apiKey];
            fValLabels[key].innerText = `${Math.round(faders[key].value * 100)}%`;
            
            if (apiKey === 'fog') {
                chrome.runtime.sendMessage({ action: "triggerVisualFX", fx: "fog", value: faders[key].value }).catch(() => {});
            }
        }
    });

    // FX state synchronization
    if (data.isStrobeActive) strobeBtn.classList.add('active');
    else strobeBtn.classList.remove('active');

    if (data.isLaserActive) laserBtn.classList.add('active');
    else laserBtn.classList.remove('active');

    if (data.isFogActive) fogBtn.classList.add('active');
    else fogBtn.classList.remove('active');

    if (spotifyBtn) {
        data.is_spotify_preferred ? 
            spotifyBtn.classList.add('active') : 
            spotifyBtn.classList.remove('active');
    }
}

function fetchState(attempts = 0) {
    // Only attempt once since we are using a single cloud API endpoint
    if (attempts >= 1) {
        connStatus.innerText = 'OFFLINE';
        connStatus.className = 'status status-offline';
        const status = "disconnected";
        if (lastStatus !== status && chrome.runtime?.id) {
            chrome.runtime.sendMessage({ status }).catch(() => {});
            lastStatus = status;
        }
        
        // The server dropped offline. Ask background to run the discovery cascade again to find the fallback
        chrome.runtime.sendMessage({ action: "forceDiscovery" }).then((response) => {
            if (response && response.url && response.url !== hostUrl) {
                hostUrl = response.url;
                console.log(`[Popup] Switched to fallback server: ${hostUrl}`);
            }
        }).catch(err => console.warn(err));
        
        return;
    }

    authFetch(`${hostUrl}/api/state`)
        .then(res => {
            if (!res.ok) throw new Error('Unhealthy portal state');
            return res.json();
        })
        .then(data => renderGridState(data))
        .catch(err => {
            console.error("[Popup] API connection failed", err);
            fetchState(attempts + 1);
        });
}



// Attach slide input listeners dynamically
Object.keys(faders).forEach(key => faders[key].addEventListener('input', (e) => {
        const valFraction = parseFloat(e.target.value);
        const param = key.replace('fader', '').toLowerCase(); // Sync naming with Kotlin backend
        
        fValLabels[key].innerText = `${Math.round(valFraction * 100)}%`;
        transmitControl(param, valFraction);

        // Immediate visual feedback for the fog slider
        if (param === 'fog' || key === 'faderFog') {
            chrome.runtime.sendMessage({ action: "triggerVisualFX", fx: "fog", value: valFraction }).catch(() => {});
        }
    }));

// Event Triggers
strobeBtn.addEventListener('click', () => {
    transmitTrigger('strobe');
});
fogBtn.addEventListener('click', () => transmitTrigger('fog'));
laserBtn.addEventListener('click', () => transmitTrigger('laser'));

const skipNextBtn = document.querySelector('[testTag="media_next_button"]');
const skipPrevBtn = document.querySelector('[testTag="media_prev_button"]');

if (skipNextBtn) {
    skipNextBtn.addEventListener('click', () => {
        if (spotifyBtn && spotifyBtn.classList.contains('active')) {
            chrome.runtime.sendMessage({ action: "triggerVisualFX", fx: "spotify_next" });
        }
    });
}
if (skipPrevBtn) {
    skipPrevBtn.addEventListener('click', () => {
        if (spotifyBtn && spotifyBtn.classList.contains('active')) {
            chrome.runtime.sendMessage({ action: "triggerVisualFX", fx: "spotify_prev" });
        }
    });
}

if (spotifyBtn) {
    spotifyBtn.addEventListener('click', () => {
        const isActive = spotifyBtn.classList.contains('active');
        transmitControl('spotify', isActive ? 0 : 1);
    });
}

playBtn.addEventListener('click', () => {
    const isSpotifyMode = spotifyBtn && spotifyBtn.classList.contains('active');
    
    if (isSpotifyMode) {
        console.log("[SYNC] Routing Play/Pause to Spotify Tab");
        chrome.runtime.sendMessage({ action: "triggerVisualFX", fx: "spotify_toggle" });
    } else {
        transmitTrigger('play');
    }
    transmitTrigger('strobe'); // Keep strobe for visual feedback
});

// Start loop
setInterval(fetchState, 1500);
fetchState();
