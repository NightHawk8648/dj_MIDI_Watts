// DJ MIDI WATTS - Core Controller and Visualizer Engine
const host = window.location.origin;

// State management
let isPlaying = false;
let currentTrack = "NO ACTIVE TRACK";
let faderValues = { sub: 0.3, low: 0.4, mid: 0.5, vocal: 0.5, high: 0.6, fog: 0.2 };
let stageTriggers = { strobe: false, fog: false, laser: false, spotify: false };
let visualMode = 0; // 0: Helix Tunnel, 1: Retro Plasma, 2: Quantum Grid
let frameCount = 0;
let lastFpsTime = Date.now();
let fps = 60;
let connectionStatus = 'offline';
let pingMs = 32;

// Professional Audio & Multi-Layer Engine
let trackLibrary = [
    { id: 'default', title: 'Astral Grid (Original Mix)', artist: 'DJ MIDI WATTS', bpm: 128, url: null, durationSec: 225 },
    { id: 'cloud-beat-128', title: 'Cloud_Beat_128.mp3', artist: 'Personal Cloud', bpm: 128, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3', durationSec: 372 },
    { id: 'ambient-swell', title: 'Ambient_Swell_Ambient.wav', artist: 'Personal Cloud', bpm: 90, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3', durationSec: 425 }
];

let activeLayers = [
    { id: 0, audio: null, trackId: '', startTime: 0, stopTime: 100, volume: 0.8, isMuted: false, sourceNode: null },
    { id: 1, audio: null, trackId: '', startTime: 0, stopTime: 100, volume: 0.8, isMuted: false, sourceNode: null },
    { id: 2, audio: null, trackId: '', startTime: 0, stopTime: 100, volume: 0.8, isMuted: false, sourceNode: null }
];

let masterTrackId = 'default';
let mediaProgressFraction = 0;
let trackDurationSec = 225;
let audioCtx = null;
let analyser = null;
let dataArray = null;

// Built-in Synthesizer State (Fallback audio)
let synthInterval = null;
let isSynthPlaying = false;

// Account & Theme State
let userAccountTier = 'Free'; // 'Free' or 'Premium'
let themesState = {
    'default': { installed: true, status: 'INSTALLED' },
    'starwars': { installed: false, status: 'NOT INSTALLED', downloading: false },
    'mauser': { installed: false, status: 'NOT INSTALLED', downloading: false }
};
let activeTheme = 'default';

// DOM Elements
const connectionIndicator = document.getElementById('connection-status');
const terminal = document.getElementById('log-terminal');
const playingTrackLabel = document.getElementById('playing-track');
const playingArtistLabel = document.getElementById('playing-artist');
const bpmDisplay = document.getElementById('track-bpm-display');
const vinylDisc = document.getElementById('vinyl-disc');
const playBtn = document.getElementById('btn-playpause');
const timeElapsed = document.getElementById('elapsed-time');
const timeDuration = document.getElementById('duration-time');
const progressBar = document.getElementById('media-progress');

// Diagnostics elements
const diagGcp = document.getElementById('diag-gcp-project');
const diagBilling = document.getElementById('diag-billing');
const diagS1 = document.getElementById('diag-s1');
const diagS2 = document.getElementById('diag-s2');
const diagPlay = document.getElementById('diag-play-api');
const diagCreds = document.getElementById('diag-creds-status');

// Canvas elements
const canvas = document.getElementById('visualizer-canvas');
const ctx = canvas.getContext('2d');
const strobeOverlay = document.getElementById('strobe-overlay');
const hudScene = document.getElementById('hud-scene');
const hudFps = document.getElementById('hud-fps');
const hudPing = document.getElementById('hud-ping');

// Log messaging
function log(message, type = 'info') {
    const timestamp = new Date().toLocaleTimeString();
    const line = document.createElement('div');
    line.className = `log-line ${type}`;
    line.textContent = `[${timestamp}] ${message}`;
    terminal.appendChild(line);
    terminal.scrollTop = terminal.scrollHeight;
}

// Format duration
function formatTime(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

// -------------------------------------------------------------
// API DISPATCH & COMMUNICATIONS
// -------------------------------------------------------------

async function fetchConfig() {
    try {
        const res = await fetch(`${host}/api/config`);
        if (!res.ok) throw new Error();
        const data = await res.json();
        
        diagGcp.textContent = data.GCP_PROJECT_ID || 'None';
        diagBilling.textContent = data.GCP_BILLING_ACCOUNT ? 'LINKED' : 'MISSING';
        
        // Hide actual secrets visually for premium design safety
        diagS1.textContent = data.UG_S1 ? 'ACTIVE (S1)' : 'VOID';
        diagS1.className = data.UG_S1 ? 'diag-value loaded' : 'diag-value missing';
        
        diagS2.textContent = data.UG_S2 ? 'ACTIVE (S2)' : 'VOID';
        diagS2.className = data.UG_S2 ? 'diag-value loaded' : 'diag-value missing';
        
        diagPlay.textContent = data.PLAY_API_KEY ? 'CONFIGURED' : 'UNSET';
        diagPlay.className = data.PLAY_API_KEY ? 'diag-value loaded' : 'diag-value missing';
        
        diagCreds.textContent = data.credentials_exist ? 'LOADED (JSON)' : 'MISSING';
        diagCreds.className = data.credentials_exist ? 'diag-value loaded' : 'diag-value missing';

        log('Vitals loaded successfully from cloud credentials database.', 'success');
    } catch (e) {
        log('Failed to reach local configuration endpoint.', 'error');
    }
}

async function transmitControl(param, value) {
    try {
        const res = await fetch(`${host}/api/control?param=${param}&value=${value}`, { method: 'POST' });
        if (res.ok) {
            log(`Sync control parameters: ${param.toUpperCase()} adjusted to ${Math.round(value * 100)}%`, 'info');
        }
    } catch (err) {
        console.warn('Control sync failed:', err);
    }
}

async function transmitTrigger(fx, value = null) {
    try {
        const query = `${host}/api/trigger?fx=${fx}${value !== null ? '&value=' + value : ''}`;
        const res = await fetch(query, { method: 'POST' });
        if (res.ok) {
            log(`Trigger active: ${fx.toUpperCase()}`, 'warn');
        }
    } catch (err) {
        console.warn('Trigger dispatch failed:', err);
    }
}

// Main polling loop
async function pollState() {
    const startTime = Date.now();
    try {
        const res = await fetch(`${host}/api/state/hardware`);
        if (!res.ok) throw new Error();
        const data = await res.json();

        pingMs = Date.now() - startTime;
        connectionStatus = 'online';
        connectionIndicator.textContent = 'ONLINE';
        connectionIndicator.className = 'status-indicator online';
        hudPing.textContent = `GRID PULSE: ${pingMs}MS`;

        // Update Star Wars vibe deck only if Star Wars theme is active
        if (activeTheme === 'starwars') {
            const sub = data.faders?.sub || 0;
            const low = data.faders?.low || 0;
            const high = data.faders?.high || 0;
            let vibeText = "Stay on target... Red Leader standing by.";
            if (sub > 0.8 || low > 0.8) {
                vibeText = "The Force is strong with this one.";
            } else if (high > 0.8) {
                vibeText = "I find your lack of faders disturbing.";
            } else if (sub < 0.2 && low < 0.2 && high < 0.2) {
                vibeText = "I've got a bad feeling about this.";
            }
            
            const vibeFeedback = document.getElementById('vibe-feedback');
            if (vibeFeedback) vibeFeedback.textContent = vibeText;
            
            const teleMemory = document.getElementById('tele-memory');
            const teleLatency = document.getElementById('tele-latency');
            const teleVault = document.getElementById('tele-vault');
            
            if (teleMemory) teleMemory.textContent = `${data.telemetry?.memory_usage || 42}%`;
            if (teleLatency) teleLatency.textContent = `${pingMs}ms`;
            if (teleVault) teleVault.textContent = `${data.vault?.vault_balance || 1500} CR`;
        }

        // Load sync values
        visualMode = data.visual_mode !== undefined ? data.visual_mode : visualMode;
        
        // Sync active state indicators
        stageTriggers.strobe = !!data.strobe_active;
        stageTriggers.laser = !!data.laser_active;
        stageTriggers.fog = !!data.fog_active;
        stageTriggers.spotify = !!data.spotify_active;

        document.getElementById('btn-strobe').classList.toggle('active', stageTriggers.strobe);
        document.getElementById('btn-laser').classList.toggle('active', stageTriggers.laser);
        document.getElementById('btn-fog').classList.toggle('active', stageTriggers.fog);
        
        const spotifyBtn = document.getElementById('btn-spotify');
        if (spotifyBtn) {
            spotifyBtn.classList.toggle('active', stageTriggers.spotify);
        }

        // Spotify metadata synchronization (if selected by hardware/ViewModel)
        if (stageTriggers.spotify && data.spotify_track) {
            if (playingTrackLabel.textContent !== data.spotify_track.title) {
                playingTrackLabel.textContent = data.spotify_track.title;
                if (playingArtistLabel) playingArtistLabel.textContent = `STUDIO ARTIST : ${data.spotify_track.artist}`;
                log(`Spotify Sync Track loaded: ${data.spotify_track.title}`, 'success');
            }
            bpmDisplay.textContent = `SPOTIFY SYNC : ${data.spotify_track.artist}`;
            
            // Sync play state animation
            const isSpotPlaying = !!data.spotify_playing;
            vinylDisc.classList.toggle('playing', isSpotPlaying);
            playBtn.textContent = isSpotPlaying ? "PAUSE" : "PLAY";
            playBtn.classList.toggle('playing', isSpotPlaying);
        }

        // Sync faders
        if (data.faders) {
            Object.keys(faders).forEach(param => {
                const apiName = param.replace('fader', '').toLowerCase();
                if (data.faders[apiName] !== undefined && document.activeElement !== faders[param]) {
                    faders[param].value = data.faders[apiName];
                    fValLabels[param].textContent = `${Math.round(data.faders[apiName] * 100)}%`;
                    faderValues[apiName] = data.faders[apiName];
                }
            });
        }
    } catch (e) {
        connectionStatus = 'offline';
        connectionIndicator.textContent = 'STANDBY';
        connectionIndicator.className = 'status-indicator';
        hudPing.textContent = 'GRID PULSE: --MS';
    }
}

// -------------------------------------------------------------
// MEDIA PLAYER SIMULATOR
// -------------------------------------------------------------

// -------------------------------------------------------------
// ACTUAL MEDIA PLAYER ENGINE & THEME MANAGER
// -------------------------------------------------------------

function initAudioCtx() {
    if (audioCtx) return;
    try {
        const AudioContextClass = window.AudioContext || window.webkitAudioContext;
        audioCtx = new AudioContextClass();
        analyser = audioCtx.createAnalyser();
        analyser.fftSize = 256;
        dataArray = new Uint8Array(analyser.frequencyBinCount);
        log("HTML5 Web Audio Context active.", "success");
    } catch (e) {
        console.warn("AudioContext init failed:", e);
    }
}

function updateTrackDropdowns() {
    for (let i = 0; i < 3; i++) {
        const select = document.getElementById(`layer-${i}-select`);
        if (!select) continue;
        
        // Preserve current selection
        const prevVal = select.value;
        
        select.innerHTML = '<option value="">-- No Source Loaded --</option>';
        select.innerHTML += '<option value="synthbeat">Built-in Synth Beat (128 BPM)</option>';
        
        trackLibrary.forEach(track => {
            select.innerHTML += `<option value="${track.id}">${track.title} [${track.bpm} BPM]</option>`;
        });
        
        select.value = prevVal;
    }
}

// Play / Pause Master
function togglePlayback() {
    initAudioCtx();
    if (audioCtx && audioCtx.state === 'suspended') {
        audioCtx.resume();
    }
    
    isPlaying = !isPlaying;
    if (isPlaying) {
        playBtn.textContent = "PAUSE";
        playBtn.classList.add('playing');
        vinylDisc.classList.add('playing');
        log('Playback started on active master deck.', 'success');
        
        // Start any layer audio that is active
        activeLayers.forEach((layer, idx) => {
            if (layer.trackId && !layer.isMuted) {
                playLayerAudio(idx);
            }
        });
        
        // Start Master animation loop
        startMasterPlaybackLoop();
    } else {
        playBtn.textContent = "PLAY";
        playBtn.classList.remove('playing');
        vinylDisc.classList.remove('playing');
        log('Playback deck paused.', 'info');
        
        // Pause all layer audio
        activeLayers.forEach(layer => {
            if (layer.audio) {
                layer.audio.pause();
            }
        });
        
        stopSynthBeat();
    }
    transmitTrigger('play');
}

function playLayerAudio(idx) {
    const layer = activeLayers[idx];
    if (layer.trackId === 'synthbeat') {
        startSynthBeat();
        return;
    }
    
    const track = trackLibrary.find(t => t.id === layer.trackId);
    if (!track) return;
    
    if (!layer.audio) {
        layer.audio = new Audio(track.url);
        layer.audio.crossOrigin = "anonymous";
        layer.audio.volume = layer.volume;
        layer.audio.loop = false; // Custom loop logic in tick
        
        // Connect to Web Audio context for visualizer spectrum analysis
        if (audioCtx && analyser) {
            try {
                layer.sourceNode = audioCtx.createMediaElementSource(layer.audio);
                layer.sourceNode.connect(analyser);
                analyser.connect(audioCtx.destination);
            } catch (err) {
                console.warn("Could not connect audio to Analyser (CORS or duplicate):", err);
            }
        }
    }
    
    // Set current time to start window
    const duration = layer.audio.duration || track.durationSec;
    const startSec = (layer.startTime / 100) * duration;
    layer.audio.currentTime = startSec;
    layer.audio.play().catch(e => console.warn("Audio playback failed:", e));
}

let masterPlaybackTimer = null;
function startMasterPlaybackLoop() {
    if (masterPlaybackTimer) clearInterval(masterPlaybackTimer);
    
    masterPlaybackTimer = setInterval(() => {
        if (!isPlaying) {
            clearInterval(masterPlaybackTimer);
            return;
        }
        
        // Loop channels tick
        activeLayers.forEach((layer, idx) => {
            if (layer.trackId && !layer.isMuted) {
                if (layer.trackId === 'synthbeat') return; // Rhythmic sequencer handles itself
                
                const track = trackLibrary.find(t => t.id === layer.trackId);
                if (!track) return;
                
                if (layer.audio && !layer.audio.paused) {
                    const duration = layer.audio.duration || track.durationSec;
                    const startSec = (layer.startTime / 100) * duration;
                    const stopSec = (layer.stopTime / 100) * duration;
                    
                    // Loop window check
                    if (layer.audio.currentTime < startSec) {
                        layer.audio.currentTime = startSec;
                    }
                    if (layer.audio.currentTime >= stopSec) {
                        layer.audio.currentTime = startSec;
                    }
                    
                    // Update visual progress for Master if Channel 1 is acting as master track
                    if (idx === 0) {
                        mediaProgressFraction = (layer.audio.currentTime - startSec) / (stopSec - startSec || 1);
                        updateMediaUI(layer.audio.currentTime, stopSec - startSec);
                    }
                } else if (layer.audio && layer.audio.paused && isPlaying) {
                    // Try to restart if paused unexpectedly
                    layer.audio.play().catch(() => {});
                }
            }
        });
        
        // fallback Master visual progress if no track loaded
        if (!activeLayers[0].trackId) {
            mediaProgressFraction += 1 / trackDurationSec;
            if (mediaProgressFraction >= 1) mediaProgressFraction = 0;
            updateMediaUI(mediaProgressFraction * trackDurationSec, trackDurationSec);
        }
    }, 100);
}

function updateMediaUI(elapsed, duration) {
    timeElapsed.textContent = formatTime(elapsed);
    timeDuration.textContent = formatTime(duration);
    progressBar.value = (elapsed / (duration || 1)) * 100;
}

// Built-in Synthesizer Sequencer (Web Audio Fallback Sound generator)
function startSynthBeat() {
    if (isSynthPlaying) return;
    initAudioCtx();
    if (!audioCtx) return;
    
    isSynthPlaying = true;
    let step = 0;
    const tempoInterval = (60 / 128) * 1000 / 4; // 16th notes at 128 BPM
    
    synthInterval = setInterval(() => {
        if (!isPlaying || !isSynthPlaying) {
            clearInterval(synthInterval);
            return;
        }
        
        // Rhythmic tick sequence
        const time = audioCtx.currentTime;
        
        // Kick Drum (Step 0, 4, 8, 12)
        if (step % 4 === 0) {
            triggerKickDrum(time);
        }
        
        // Hi-Hat (Step 2, 6, 10, 14)
        if (step % 4 === 2) {
            triggerHiHat(time);
        }
        
        // Rhythmic Synth Melody
        if (step % 2 === 0) {
            const melody = [110, 130, 146, 165, 110, 165, 196, 220];
            const freq = melody[(step / 2) % melody.length];
            triggerSynthPluck(time, freq);
        }
        
        step = (step + 1) % 16;
    }, tempoInterval);
    
    log("Synthesizer Sequence started: 128 BPM Synthwave Loop.", "success");
}

function stopSynthBeat() {
    isSynthPlaying = false;
    if (synthInterval) clearInterval(synthInterval);
}

function triggerKickDrum(time) {
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.connect(gain);
    gain.connect(analyser || audioCtx.destination);
    
    osc.frequency.setValueAtTime(150, time);
    osc.frequency.exponentialRampToValueAtTime(40, time + 0.15);
    
    gain.gain.setValueAtTime(1.0, time);
    gain.gain.exponentialRampToValueAtTime(0.01, time + 0.15);
    
    osc.start(time);
    osc.stop(time + 0.16);
}

function triggerHiHat(time) {
    const bufferSize = audioCtx.sampleRate * 0.05;
    const buffer = audioCtx.createBuffer(1, bufferSize, audioCtx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
        data[i] = Math.random() * 2 - 1;
    }
    
    const noise = audioCtx.createBufferSource();
    noise.buffer = buffer;
    
    const filter = audioCtx.createBiquadFilter();
    filter.type = "highpass";
    filter.frequency.value = 7000;
    
    const gain = audioCtx.createGain();
    noise.connect(filter);
    filter.connect(gain);
    gain.connect(analyser || audioCtx.destination);
    
    gain.gain.setValueAtTime(0.3, time);
    gain.gain.exponentialRampToValueAtTime(0.01, time + 0.05);
    
    noise.start(time);
    noise.stop(time + 0.06);
}

function triggerSynthPluck(time, freq) {
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = "sawtooth";
    osc.frequency.setValueAtTime(freq, time);
    
    osc.connect(gain);
    gain.connect(analyser || audioCtx.destination);
    
    gain.gain.setValueAtTime(0.2, time);
    gain.gain.exponentialRampToValueAtTime(0.01, time + 0.2);
    
    osc.start(time);
    osc.stop(time + 0.22);
}

// Drag & Drop / File upload handling
function loadLocalFiles(files) {
    initAudioCtx();
    let filesLoaded = 0;
    
    Array.from(files).forEach((file, index) => {
        const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
        const allowed = ['.mp3', '.wav', '.ogg', '.m4a', '.aac'];
        if (!allowed.includes(ext)) {
            log(`File format rejected: ${file.name}. Only .mp3, .wav, .ogg, .m4a, and .aac are allowed.`, 'error');
            return;
        }
        
        const url = URL.createObjectURL(file);
        const newTrack = {
            id: `local-${Date.now()}-${index}`,
            title: file.name,
            artist: 'Local Upload',
            bpm: 120 + Math.floor(Math.random() * 15),
            url: url,
            durationSec: 180
        };
        
        const tempAudio = new Audio(url);
        tempAudio.addEventListener('loadedmetadata', () => {
            newTrack.durationSec = Math.round(tempAudio.duration);
        });
        
        trackLibrary.push(newTrack);
        filesLoaded++;
        log(`Loaded audio track: ${file.name}`, 'success');
    });
    
    if (filesLoaded > 0) {
        updateTrackDropdowns();
        const lastTrack = trackLibrary[trackLibrary.length - 1];
        masterTrackId = lastTrack.id;
        playingTrackLabel.textContent = lastTrack.title;
        playingArtistLabel.textContent = `STUDIO ARTIST : ${lastTrack.artist}`;
        bpmDisplay.textContent = `TEMPO CONFIG : ${lastTrack.bpm} BPM / STEREO 48KHZ`;
        trackDurationSec = lastTrack.durationSec;
        mediaProgressFraction = 0;
        updateMediaUI(0, trackDurationSec);
    }
}

// Spotify Track Loader Mock API
function loadSpotifyTrack(urlOrId) {
    initAudioCtx();
    if (!urlOrId.trim()) return;
    
    let trackId = urlOrId.trim();
    if (trackId.includes('spotify.com/track/')) {
        trackId = trackId.split('spotify.com/track/')[1].split('?')[0];
    }
    
    log(`Connecting to Spotify Web Service API (caching)...`, 'info');
    
    setTimeout(() => {
        const spotifyTracks = [
            { id: 'spot-1', title: 'Starlight Overdrive', artist: 'Neon Voyager', bpm: 124, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3' },
            { id: 'spot-2', title: 'Synthesized Dreams', artist: 'Chroma Horizon', bpm: 110, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3' },
            { id: 'spot-3', title: 'Midnight Velocity', artist: 'Delta Phase', bpm: 142, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3' }
        ];
        
        const selected = spotifyTracks[Math.floor(Math.random() * spotifyTracks.length)];
        const newTrack = {
            id: `spotify-${trackId}`,
            title: `[Spotify] ${selected.title}`,
            artist: selected.artist,
            bpm: selected.bpm,
            url: selected.url,
            durationSec: 300,
            isSpotify: true
        };
        
        trackLibrary.push(newTrack);
        updateTrackDropdowns();
        
        masterTrackId = newTrack.id;
        playingTrackLabel.textContent = newTrack.title;
        playingArtistLabel.textContent = `SPOTIFY STREAM : ${newTrack.artist}`;
        bpmDisplay.textContent = `SPOTIFY BPM : ${newTrack.bpm} BPM`;
        trackDurationSec = 300;
        mediaProgressFraction = 0;
        updateMediaUI(0, 300);
        
        log(`Spotify track loaded successfully: ${selected.title}`, 'success');
    }, 1000);
}

// Download Spotify track with premium permissions check
function downloadSpotifyTrack() {
    const currentTrack = trackLibrary.find(t => t.id === masterTrackId);
    if (!currentTrack || !currentTrack.isSpotify) {
        log("No Spotify track currently loaded to download.", "warn");
        return;
    }
    
    log(`Authorizing download permissions for: ${currentTrack.title}...`, 'info');
    
    setTimeout(() => {
        if (userAccountTier === 'Premium') {
            log("Authorization SUCCESS. Premium License Valid.", "success");
            log(`Downloading Spotify Audio archive: ${currentTrack.title}.mp3`, "success");
            
            const a = document.createElement('a');
            a.href = currentTrack.url;
            a.download = `${currentTrack.title.replace('[Spotify] ', '')}.mp3`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
        } else {
            log("AUTHORIZATION DENIED: Spotify track caching is restricted on Free accounts.", "error");
            alert("🔒 Access Denied\n\nSpotify offline downloads are restricted to Premium accounts only. Please upgrade your subscription in the Global Settings panel at the bottom.");
        }
    }, 600);
}

// Theme Installer Download Sequence
function simulateDownload(themeId) {
    if (themesState[themeId].installed || themesState[themeId].downloading) return;
    
    themesState[themeId].downloading = true;
    const btn = document.getElementById(`btn-action-${themeId}`);
    const statusText = document.getElementById(`status-${themeId}`);
    const progressContainer = document.getElementById(`dl-bar-${themeId}-container`);
    const progressBar = document.getElementById(`dl-bar-${themeId}`);
    
    if (btn) btn.disabled = true;
    if (statusText) statusText.textContent = "DOWNLOADING...";
    if (progressContainer) progressContainer.classList.remove('hidden');
    
    let progress = 0;
    const interval = setInterval(() => {
        progress += 4;
        if (progressBar) progressBar.style.width = `${progress}%`;
        
        if (progress >= 100) {
            clearInterval(interval);
            themesState[themeId].downloading = false;
            themesState[themeId].installed = true;
            themesState[themeId].status = 'INSTALLED';
            
            if (statusText) {
                statusText.textContent = "INSTALLED";
                statusText.classList.add('success');
            }
            if (progressContainer) progressContainer.classList.add('hidden');
            
            if (btn) {
                btn.disabled = false;
                btn.textContent = "APPLY";
                btn.classList.add('theme-action-btn-apply');
            }
            log(`MIDI theme package [${themeId.toUpperCase()}] installed successfully.`, 'success');
        }
    }, 100);
}

function applyTheme(themeId) {
    if (!themesState[themeId].installed) {
        simulateDownload(themeId);
        return;
    }
    
    document.body.classList.remove('theme-starwars', 'theme-mauser');
    
    const activeBadge = document.getElementById('active-theme-badge');
    const starwarsBlock = document.getElementById('starwars-telemetry-block');
    
    document.querySelectorAll('.theme-action-btn').forEach(b => b.classList.remove('active'));
    document.getElementById('btn-apply-default').classList.remove('active');
    
    activeTheme = themeId;
    
    if (themeId === 'default') {
        document.getElementById('btn-apply-default').classList.add('active');
        if (activeBadge) activeBadge.textContent = "DEFAULT NEON BOARD";
        if (starwarsBlock) starwarsBlock.classList.add('hidden');
        log("Applied Theme: Default Neon Board", "success");
    } else if (themeId === 'starwars') {
        document.body.classList.add('theme-starwars');
        document.getElementById('btn-action-starwars').classList.add('active');
        if (activeBadge) activeBadge.textContent = "STAR WARS THEME";
        if (starwarsBlock) starwarsBlock.classList.remove('hidden');
        log("Applied Theme: Star Wars Galactic Board", "success");
    } else if (themeId === 'mauser') {
        document.body.classList.add('theme-mauser');
        document.getElementById('btn-action-mauser').classList.add('active');
        if (activeBadge) activeBadge.textContent = "MAUSER THEME";
        if (starwarsBlock) starwarsBlock.classList.add('hidden');
        log("Applied Theme: Mauser Industrial Board", "success");
    }
}

// -------------------------------------------------------------
// BIND EVENTS
// -------------------------------------------------------------

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

// Hook fader inputs
Object.keys(faders).forEach(id => {
    faders[id].addEventListener('input', (e) => {
        const val = parseFloat(e.target.value);
        fValLabels[id].textContent = `${Math.round(val * 100)}%`;
        const key = id.replace('fader', '').toLowerCase();
        faderValues[key] = val;
        transmitControl(key, val);
    });
});

// Play/Pause button click
playBtn.addEventListener('click', togglePlayback);

// Previous / Next buttons
document.getElementById('btn-prev').addEventListener('click', () => {
    mediaProgressFraction = 0;
    if (activeLayers[0].audio) {
        activeLayers[0].audio.currentTime = 0;
    }
    updateMediaUI(0, trackDurationSec);
    log('Skipped to previous track.', 'info');
    transmitTrigger('prev');
});

document.getElementById('btn-next').addEventListener('click', () => {
    mediaProgressFraction = 0;
    if (activeLayers[0].audio) {
        activeLayers[0].audio.currentTime = 0;
    }
    updateMediaUI(0, trackDurationSec);
    log('Skipped to next track.', 'info');
    transmitTrigger('next');
});

// Trigger FX grid buttons
document.getElementById('btn-strobe').addEventListener('click', (e) => {
    stageTriggers.strobe = !stageTriggers.strobe;
    e.target.classList.toggle('active', stageTriggers.strobe);
    transmitTrigger('strobe', stageTriggers.strobe ? 1 : 0);
});

document.getElementById('btn-fog').addEventListener('click', (e) => {
    stageTriggers.fog = !stageTriggers.fog;
    e.target.classList.toggle('active', stageTriggers.fog);
    transmitTrigger('fog', stageTriggers.fog ? 1 : 0);
});

document.getElementById('btn-laser').addEventListener('click', (e) => {
    stageTriggers.laser = !stageTriggers.laser;
    e.target.classList.toggle('active', stageTriggers.laser);
    transmitTrigger('laser', stageTriggers.laser ? 1 : 0);
});

document.getElementById('btn-spotify').addEventListener('click', (e) => {
    stageTriggers.spotify = !stageTriggers.spotify;
    e.target.classList.toggle('active', stageTriggers.spotify);
    transmitControl('spotify', stageTriggers.spotify ? 1 : 0);
});

// Visual mode buttons
document.querySelectorAll('.vis-mode-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
        document.querySelectorAll('.vis-mode-btn').forEach(b => b.classList.remove('active'));
        e.target.classList.add('active');
        visualMode = parseInt(e.target.getAttribute('data-mode'));
        const modes = ['HELIX TUNNEL', 'PLASMA RETRO', 'QUANTUM GRID'];
        hudScene.textContent = `SCENE: ${modes[visualMode]}`;
        log(`Switched visualization theme to: ${modes[visualMode]}`, 'info');
    });
});

// Double tap canvas for fullscreen
canvas.addEventListener('dblclick', () => {
    if (!document.fullscreenElement) {
        canvas.parentElement.requestFullscreen().catch(err => {
            console.error(`Fullscreen request failed: ${err.message}`);
        });
    } else {
        document.exitFullscreen();
    }
});

// Hook new drag and drop events
const dropZone = document.getElementById('drop-zone');
if (dropZone) {
    dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropZone.classList.add('dragover');
    });
    
    dropZone.addEventListener('dragleave', () => {
        dropZone.classList.remove('dragover');
    });
    
    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropZone.classList.remove('dragover');
        if (e.dataTransfer.files.length > 0) {
            loadLocalFiles(e.dataTransfer.files);
        }
    });
    
    dropZone.addEventListener('click', () => {
        document.getElementById('media-file-input').click();
    });
}

const fileInput = document.getElementById('media-file-input');
if (fileInput) {
    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            loadLocalFiles(e.target.files);
        }
    });
}

// Hook Spotify actions
const btnSpotifyLoad = document.getElementById('btn-spotify-load');
const spotifyInput = document.getElementById('spotify-track-input');
if (btnSpotifyLoad && spotifyInput) {
    btnSpotifyLoad.addEventListener('click', () => {
        loadSpotifyTrack(spotifyInput.value);
    });
}

const btnSpotifyDownload = document.getElementById('btn-spotify-download');
if (btnSpotifyDownload) {
    btnSpotifyDownload.addEventListener('click', () => {
        downloadSpotifyTrack();
    });
}

// Hook Personal Cloud buttons
document.querySelectorAll('.cloud-track-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
        const url = e.target.getAttribute('data-url');
        const name = e.target.getAttribute('data-name');
        log(`Loading audio from cloud storage cache: ${name}`, 'info');
        
        let existing = trackLibrary.find(t => t.id === name);
        if (!existing) {
            existing = {
                id: name,
                title: name,
                artist: 'Personal Cloud',
                bpm: 128,
                url: url,
                durationSec: 250
            };
            trackLibrary.push(existing);
            updateTrackDropdowns();
        }
        
        masterTrackId = existing.id;
        playingTrackLabel.textContent = existing.title;
        playingArtistLabel.textContent = `CLOUD STORE : ${existing.artist}`;
        bpmDisplay.textContent = `TEMPO CONFIG : ${existing.bpm} BPM`;
        trackDurationSec = existing.durationSec;
        mediaProgressFraction = 0;
        updateMediaUI(0, trackDurationSec);
    });
});

// Hook Multi-Layer Selects, Mute, Volume, Start/Stop sliders
for (let i = 0; i < 3; i++) {
    const select = document.getElementById(`layer-${i}-select`);
    if (select) {
        select.addEventListener('change', (e) => {
            const val = e.target.value;
            activeLayers[i].trackId = val;
            
            if (activeLayers[i].audio) {
                activeLayers[i].audio.pause();
                activeLayers[i].audio = null;
                activeLayers[i].sourceNode = null;
            }
            
            if (val) {
                log(`Channel ${i+1} routed to track: ${val === 'synthbeat' ? 'Synthesizer Beat' : val}`, 'info');
                if (isPlaying) playLayerAudio(i);
            }
        });
    }
    
    const muteBtn = document.getElementById(`layer-${i}-mute`);
    if (muteBtn) {
        muteBtn.addEventListener('click', () => {
            const layer = activeLayers[i];
            layer.isMuted = !layer.isMuted;
            muteBtn.classList.toggle('muted', layer.isMuted);
            
            if (layer.audio) {
                if (layer.isMuted) {
                    layer.audio.pause();
                } else if (isPlaying) {
                    layer.audio.play().catch(() => {});
                }
            }
            
            if (layer.trackId === 'synthbeat') {
                if (layer.isMuted) stopSynthBeat();
                else if (isPlaying) startSynthBeat();
            }
            
            log(`Channel ${i+1} ${layer.isMuted ? 'muted' : 'unmuted'}.`, 'info');
        });
    }
    
    const startSlider = document.getElementById(`layer-${i}-start`);
    const endSlider = document.getElementById(`layer-${i}-end`);
    const valLabel = document.getElementById(`layer-${i}-vals`);
    
    const updateLoopLabels = () => {
        const start = parseFloat(startSlider.value);
        const end = parseFloat(endSlider.value);
        
        if (start > end) {
            startSlider.value = end;
        }
        
        activeLayers[i].startTime = parseFloat(startSlider.value);
        activeLayers[i].stopTime = parseFloat(endSlider.value);
        
        const track = trackLibrary.find(t => t.id === activeLayers[i].trackId);
        const duration = track ? track.durationSec : 100;
        
        const startSec = (activeLayers[i].startTime / 100) * duration;
        const endSec = (activeLayers[i].stopTime / 100) * duration;
        
        valLabel.textContent = `${startSec.toFixed(1)}s - ${endSec.toFixed(1)}s`;
    };
    
    if (startSlider && endSlider) {
        startSlider.addEventListener('input', updateLoopLabels);
        endSlider.addEventListener('input', updateLoopLabels);
    }
}

// Hook Settings options: Account Tier & Themes
const btnTierFree = document.getElementById('btn-tier-free');
const btnTierPremium = document.getElementById('btn-tier-premium');
const badgeAccount = document.getElementById('account-tier-badge');

if (btnTierFree && btnTierPremium && badgeAccount) {
    btnTierFree.addEventListener('click', () => {
        userAccountTier = 'Free';
        btnTierFree.classList.add('active');
        btnTierPremium.classList.remove('active');
        badgeAccount.textContent = 'FREE ACCOUNT';
        badgeAccount.classList.remove('success');
        log("Subscription mode updated: Free account active.", 'info');
    });
    
    btnTierPremium.addEventListener('click', () => {
        userAccountTier = 'Premium';
        btnTierPremium.classList.add('active');
        btnTierFree.classList.remove('active');
        badgeAccount.textContent = 'PREMIUM ACCOUNT';
        badgeAccount.classList.add('success');
        log("Subscription mode updated: Premium account activated.", 'success');
    });
}

// Themes setup
const btnDefault = document.getElementById('btn-apply-default');
if (btnDefault) {
    btnDefault.addEventListener('click', () => applyTheme('default'));
}

const btnStarwars = document.getElementById('btn-action-starwars');
if (btnStarwars) {
    btnStarwars.addEventListener('click', () => {
        if (themesState.starwars.installed) {
            applyTheme('starwars');
        } else {
            simulateDownload('starwars');
        }
    });
}

const btnMauser = document.getElementById('btn-action-mauser');
if (btnMauser) {
    btnMauser.addEventListener('click', () => {
        if (themesState.mauser.installed) {
            applyTheme('mauser');
        } else {
            simulateDownload('mauser');
        }
    });
}

// -------------------------------------------------------------
// HTML5 PROCEDURAL CANVAS VISUALIZER
// -------------------------------------------------------------

function resizeCanvas() {
    canvas.width = canvas.parentElement.clientWidth;
    canvas.height = canvas.parentElement.clientHeight;
}
window.addEventListener('resize', resizeCanvas);
resizeCanvas();

let tick = 0;

function drawVisualizer() {
    requestAnimationFrame(drawVisualizer);
    
    // FPS tracking
    frameCount++;
    const now = Date.now();
    if (now - lastFpsTime >= 1000) {
        fps = frameCount;
        frameCount = 0;
        lastFpsTime = now;
        hudFps.textContent = `HD RENDER ${fps}FPS`;
    }

    const w = canvas.width;
    const h = canvas.height;
    const cx = w / 2;
    const cy = h / 2;
    const diag = Math.max(cx, cy);

    // Clear background
    ctx.fillStyle = '#020617';
    ctx.fillRect(0, 0, w, h);

    // Retreive dynamic EQ energy levels
    let low = Math.max(0.1, faderValues.low);
    let mid = Math.max(0.1, faderValues.mid);
    let high = Math.max(0.1, faderValues.high);
    let sub = Math.max(0.1, faderValues.sub);

    // Mix real frequency levels from Analyser
    if (analyser && isPlaying) {
        analyser.getByteFrequencyData(dataArray);
        let sumSub = 0, sumLow = 0, sumMid = 0, sumHigh = 0;
        
        for (let i = 0; i < 8; i++) sumSub += dataArray[i];
        for (let i = 8; i < 24; i++) sumLow += dataArray[i];
        for (let i = 24; i < 64; i++) sumMid += dataArray[i];
        for (let i = 64; i < 128; i++) sumHigh += dataArray[i];
        
        const valSub = sumSub / (8 * 255);
        const valLow = sumLow / (16 * 255);
        const valMid = sumMid / (40 * 255);
        const valHigh = sumHigh / (64 * 255);
        
        sub = Math.max(sub * 0.3, valSub * 1.5);
        low = Math.max(low * 0.3, valLow * 1.5);
        mid = Math.max(mid * 0.3, valMid * 1.5);
        high = Math.max(high * 0.3, valHigh * 1.5);
    }

    const speed = isPlaying ? 0.05 : 0.01;
    tick += speed;

    if (visualMode === 0) { // HELIX TUNNEL
        const rings = 12;
        for (let i = 0; i < rings; i++) {
            let depth = (i / rings + tick * 0.4) % 1.0;
            if (depth < 0) depth += 1.0;
            
            const radius = depth * diag * 0.95;
            const alpha = (1.0 - depth) * (0.2 + low * 0.8);
            const rot = tick * 0.3 + i * 0.25;
            const vertices = 5;
            const points = [];

            for (let v = 0; v < vertices; v++) {
                const angle = (v * 2 * Math.PI / vertices) + rot;
                const px = cx + Math.cos(angle) * radius;
                const py = cy + Math.sin(angle) * radius;
                points.push({ x: px, y: py });
            }

            ctx.strokeStyle = `rgba(0, 255, 204, ${alpha})`;
            ctx.lineWidth = Math.max(1, (1.0 - depth) * 4);
            ctx.beginPath();
            ctx.moveTo(points[0].x, points[0].y);
            for (let p = 1; p < vertices; p++) {
                ctx.lineTo(points[p].x, points[p].y);
            }
            ctx.closePath();
            ctx.stroke();
        }
    } else if (visualMode === 1) { // RETRO PLASMA
        const blobs = 4;
        for (let i = 0; i < blobs; i++) {
            const rx = cx * 0.4 * Math.sin(tick * 0.8 + i * 1.5);
            const ry = cy * 0.4 * Math.cos(tick * 0.6 + i * 2.1);
            const baseRad = (30 + mid * 40) * (1.0 + 0.25 * Math.sin(tick * 3.0 + i));

            const grad = ctx.createRadialGradient(cx + rx, cy + ry, 2, cx + rx, cy + ry, baseRad);
            grad.addColorStop(0, `rgba(255, 0, 127, ${0.5 * mid})`);
            grad.addColorStop(0.5, `rgba(0, 255, 204, ${0.2 * sub})`);
            grad.addColorStop(1, 'rgba(0, 0, 0, 0)');

            ctx.fillStyle = grad;
            ctx.beginPath();
            ctx.arc(cx + rx, cy + ry, baseRad, 0, 2 * Math.PI);
            ctx.fill();
        }
    } else if (visualMode === 2) { // QUANTUM GRID
        const cols = 8;
        const rows = 6;
        const spX = w / (cols + 1);
        const spY = h / (rows + 1);

        for (let x = 1; x <= cols; x++) {
            for (let y = 1; y <= rows; y++) {
                const px = x * spX;
                const py = y * spY;

                const dx = Math.sin(tick * 2.4 + x * 0.5 + y * 0.3) * 12 * high;
                const dy = Math.cos(tick * 1.8 + x * 0.2 + y * 0.6) * 12 * high;
                const size = (2 + high * 6) * (1.0 + 0.4 * Math.sin(tick * 4.0 + (x + y)));

                ctx.fillStyle = (x + y) % 2 === 0 ? '#00ffcc' : '#ff007f';
                ctx.beginPath();
                ctx.arc(px + dx, py + dy, Math.max(1, size), 0, 2 * Math.PI);
                ctx.fill();
            }
        }
    }

    // Overlay filters
    // 1. STROBE FLASH
    if (stageTriggers.strobe && isPlaying) {
        const frameInterval = 6; // flash every 6 frames
        const strobeState = Math.floor(Date.now() / 80) % 2 === 0;
        strobeOverlay.style.opacity = strobeState ? '0.25' : '0';
    } else {
        strobeOverlay.style.opacity = '0';
    }

    // 2. LASER BURST BEAMS
    if (stageTriggers.laser) {
        const beams = 3;
        for (let i = 0; i < beams; i++) {
            const sweepX = cx + Math.sin(tick * 3.0 + i * 1.1) * (cx * 0.9);
            ctx.strokeStyle = 'rgba(255, 0, 127, 0.7)';
            ctx.lineWidth = 4;
            ctx.beginPath();
            ctx.moveTo(cx, 0);
            ctx.lineTo(sweepX, h);
            ctx.stroke();
            
            // Neon glow shadow
            ctx.strokeStyle = 'rgba(255, 0, 127, 0.2)';
            ctx.lineWidth = 12;
            ctx.beginPath();
            ctx.moveTo(cx, 0);
            ctx.lineTo(sweepX, h);
            ctx.stroke();
        }
    }

    // 3. FOG AND SMOKE SCROLLING
    const fogDensity = faderValues.fog;
    if (stageTriggers.fog || fogDensity > 0.05) {
        const offset = (tick * 15) % h;
        const grad = ctx.createLinearGradient(0, offset, 0, h + offset);
        grad.addColorStop(0, 'rgba(255, 255, 255, 0)');
        grad.addColorStop(0.3, `rgba(255, 255, 255, ${0.1 * fogDensity})`);
        grad.addColorStop(0.7, `rgba(255, 255, 255, ${0.05 * fogDensity})`);
        grad.addColorStop(1, 'rgba(255, 255, 255, 0)');
        
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, w, h);
        
        const grad2 = ctx.createLinearGradient(0, offset - h, 0, offset);
        grad2.addColorStop(0, 'rgba(255, 255, 255, 0)');
        grad2.addColorStop(0.3, `rgba(255, 255, 255, ${0.1 * fogDensity})`);
        grad2.addColorStop(0.7, `rgba(255, 255, 255, ${0.05 * fogDensity})`);
        grad2.addColorStop(1, 'rgba(255, 255, 255, 0)');
        
        ctx.fillStyle = grad2;
        ctx.fillRect(0, 0, w, h);
    }
}

// -------------------------------------------------------------
// PREBOOT DISCOVERY & HARDWARE REGISTER
// -------------------------------------------------------------

const prebootBtStatus = document.getElementById('preboot-bt-status');
const prebootVirtualStatus = document.getElementById('preboot-virtual-status');
const prebootRoutingStatus = document.getElementById('preboot-routing-status');
const prebootDeviceList = document.getElementById('preboot-device-list');

async function fetchPreboot() {
    try {
        const res = await fetch(`${host}/api/workspace/preboot`);
        if (!res.ok) throw new Error();
        const data = await res.json();

        // 1. Update overall statuses
        if (prebootBtStatus) {
            if (data.bluetooth_enabled) {
                prebootBtStatus.textContent = 'ENABLED';
                prebootBtStatus.className = 'diag-value loaded';
            } else {
                prebootBtStatus.textContent = 'DISABLED';
                prebootBtStatus.className = 'diag-value missing';
            }
        }

        if (prebootVirtualStatus) {
            if (data.virtual_midi_installed) {
                prebootVirtualStatus.textContent = 'ACTIVE';
                prebootVirtualStatus.className = 'diag-value loaded';
            } else {
                prebootVirtualStatus.textContent = 'MISSING';
                prebootVirtualStatus.className = 'diag-value missing';
            }
        }

        if (prebootRoutingStatus) {
            if (data.loopmidi_running) {
                prebootRoutingStatus.textContent = 'ONLINE';
                prebootRoutingStatus.className = 'diag-value loaded';
            } else {
                prebootRoutingStatus.textContent = 'STANDBY';
                prebootRoutingStatus.className = 'diag-value missing';
            }
        }

        // 2. Render Discovered Devices
        if (prebootDeviceList) {
            prebootDeviceList.innerHTML = '';
            let devicesAdded = 0;

            const addDeviceItem = (name, type, status, isWarning = false) => {
                const devItem = document.createElement('div');
                devItem.className = 'device-item';
                
                const infoDiv = document.createElement('div');
                infoDiv.style.display = 'flex';
                infoDiv.style.alignItems = 'center';
                
                const nameSpan = document.createElement('span');
                nameSpan.className = 'device-name';
                nameSpan.textContent = name;
                
                const typeSpan = document.createElement('span');
                typeSpan.className = 'device-type';
                typeSpan.textContent = type;
                
                infoDiv.appendChild(nameSpan);
                infoDiv.appendChild(typeSpan);
                
                const statusSpan = document.createElement('span');
                statusSpan.className = `device-status${isWarning ? ' warning' : ''}`;
                statusSpan.textContent = status;
                
                devItem.appendChild(infoDiv);
                devItem.appendChild(statusSpan);
                
                prebootDeviceList.appendChild(devItem);
                devicesAdded++;
            };

            if (data.devices) {
                // Render USB devices
                if (data.devices.usb && data.devices.usb.length > 0) {
                    data.devices.usb.forEach(d => {
                        addDeviceItem(d.name, d.type || 'USB', d.status || 'OK', d.needs_driver);
                    });
                }
                // Render Bluetooth devices
                if (data.devices.bluetooth && data.devices.bluetooth.length > 0) {
                    data.devices.bluetooth.forEach(d => {
                        addDeviceItem(d.name, 'Bluetooth', d.connected ? 'OK' : 'PAIRED', !d.connected);
                    });
                }
                // Render Virtual devices
                if (data.devices.virtual && data.devices.virtual.length > 0) {
                    data.devices.virtual.forEach(d => {
                        if (d.installed) {
                            const statusStr = d.running ? 'RUNNING' : 'STANDBY';
                            addDeviceItem(d.name, 'Virtual', statusStr, !d.running);
                        }
                    });
                }
            }

            if (devicesAdded === 0) {
                const emptyDiv = document.createElement('div');
                emptyDiv.className = 'device-item empty';
                emptyDiv.textContent = 'No active devices registered. Run preboot script.';
                prebootDeviceList.appendChild(emptyDiv);
            }
        }

        // Print warnings to terminal log if any are new
        if (data.warnings && data.warnings.length > 0) {
            if (!window.loggedWarnings) window.loggedWarnings = new Set();
            data.warnings.forEach(w => {
                if (!window.loggedWarnings.has(w)) {
                    log(`[PREBOOT] ${w}`, 'warn');
                    window.loggedWarnings.add(w);
                }
            });
        }

    } catch (e) {
        console.warn('Failed to fetch preboot status:', e);
    }
}

// -------------------------------------------------------------
// INITIALIZE
// -------------------------------------------------------------

log('Uplinking to active grid node configurations...', 'info');
fetchConfig();
fetchPreboot();
pollState();
setInterval(pollState, 1500);
setInterval(fetchPreboot, 5000);

updateTrackDropdowns();
updateMediaUI(0, trackDurationSec);

// Apply initial default theme
applyTheme('default');
