/**
 * DJ MIDI WATTS - Visual FX Injection Engine
 * Non-intrusive ambient effects layer
 */

// ============================================================================
// SAFETY SAFEGUARDS: Prevent execution on banking, secure, or conflicting sites
// ============================================================================
(function() {
const currentUrl = window.location.href;
const restrictedPatterns = [
    /bank/i,
    /finance/i,
    /paypal\.com/i,
    /stripe\.com/i,
    /secure/i,
    /checkout/i,
    /auth0\.com/i,
    /okta\.com/i,
    /apple\.com/i,
    // Add specific competing Web MIDI platforms if needed
    /soundtrap\.com/i,
    /bandlab\.com/i
];

if (restrictedPatterns.some(pattern => pattern.test(currentUrl))) {
    // Silently abort execution on restricted domains
    return;
}

// Initialize canvas/overlay wrappers safely on the host page DOM
const fxContainer = document.createElement('div');
fxContainer.id = 'midi-watts-fx-layer';
fxContainer.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    pointer-events: none;
    z-index: 999999;
    overflow: hidden;
    --watts-theme-color: #00FFCC;
`;
document.body.appendChild(fxContainer);

// Inject embedded CSS keyframes for subtle glitches, strobes, and lasers
const style = document.createElement('style');
style.textContent = `
    @keyframes subtleStrobe {
        0%, 100% { background: rgba(255, 255, 255, 0); backdrop-filter: none; transform: translate(0,0); }
        33% { background: rgba(255, 255, 255, 0.015); transform: translate(0.5px, -0.5px); }
        66% { background: rgba(0, 255, 204, 0.005); backdrop-filter: contrast(1.01) saturate(1.05); transform: translate(-0.5px, 0.5px); }
        66% { background: var(--watts-theme-color); opacity: 0.01; backdrop-filter: contrast(1.01) saturate(1.05); transform: translate(-0.5px, 0.5px); }
    }
    .watts-strobe-active {
        animation: subtleStrobe 0.15s infinite ease-in-out;
    }
    
    .watts-fog-overlay {
        position: absolute;
        width: 200%;
        height: 200%;
        background: radial-gradient(circle at center, rgba(255,255,255,0.08) 0%, rgba(255,255,255,0) 70%);
        filter: blur(20px);
        opacity: 0;
        transition: opacity 0.5s ease-out;
        transform: translate(-25%, -25%);
        pointer-events: none;
    }

    .watts-laser-line {
        position: absolute;
        height: 1px;
        background: linear-gradient(90deg, transparent, rgba(255, 0, 127, 0.3), rgba(0, 240, 255, 0.3), transparent);
        box-shadow: 0 0 3px rgba(255, 0, 127, 0.2);
        background: linear-gradient(90deg, transparent, var(--watts-theme-color), transparent);
        box-shadow: 0 0 5px var(--watts-theme-color);
        opacity: 0.2;
        width: 100%;
        top: 20%;
        transform: scaleY(0.5);
        transition: all 0.6s cubic-bezier(0.1, 0.8, 0.3, 1);
        pointer-events: none;
    }

    @keyframes watts-thump {
        0%, 100% { transform: translateY(0); }
        50% { transform: translateY(0.8px); }
    }
    .watts-body-thump {
        animation: watts-thump 0.15s infinite ease-in-out !important;
    }
`;
document.head.appendChild(style);

// Listen for commands broadcast from background.js
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    const { fx, value, color } = request;

    // Update the local theme resonance if a new color is provided
    if (color) {
        const hexRegex = /^#(?:[0-9a-fA-F]{3}){1,2}$/;
        const safeColor = hexRegex.test(color) ? color : '#00FFCC';
        fxContainer.style.setProperty('--watts-theme-color', safeColor);
    }

    if (fx === 'strobe') {
        triggerStrobeGlitch(value > 0.9);
    } else if (fx === 'fog') {
        // dynamic values accepted between 0 and 1
        adjustFogger(value !== null ? value : 0.5);
    } else if (fx === 'laser') {
        // Use passed color or fallback to current theme variable
        triggerLaserShow(color || getComputedStyle(fxContainer).getPropertyValue('--watts-theme-color'));
    } else if (fx === 'spotify_toggle' && window.location.hostname.includes('spotify.com')) {
        // Target the Spotify Web Player play/pause button
        const spotBtn = document.querySelector('[data-testid="control-button-playpause"]') || 
                        document.querySelector('button[aria-label="Play"]') || 
                        document.querySelector('button[aria-label="Pause"]');
        if (spotBtn) spotBtn.click();
    } else if (fx === 'spotify_next' && window.location.hostname.includes('spotify.com')) {
        const nextBtn = document.querySelector('[data-testid="control-button-skip-forward"]') || 
                        document.querySelector('button[aria-label="Next"]');
        if (nextBtn) nextBtn.click();
    } else if (fx === 'spotify_prev' && window.location.hostname.includes('spotify.com')) {
        const prevBtn = document.querySelector('[data-testid="control-button-skip-back"]') || 
                        document.querySelector('button[aria-label="Previous"]');
        if (prevBtn) prevBtn.click();
    }
});

// Metadata Scraper: Periodically extract track info and send to background relay
setInterval(() => {
    if (window.location.hostname.includes('spotify.com')) {
        const title = document.querySelector('[data-testid="context-item-info-title"]')?.innerText;
        const artist = document.querySelector('[data-testid="context-item-info-subtitles"]')?.innerText;
        if (title && artist) {
            chrome.runtime.sendMessage({ action: "spotify_metadata", title, artist });
        }
    }
}, 3000);

// 1. STROBE + GLITCH EFFECT
let strobeTimeout;
function triggerStrobeGlitch(isBassHeavy = false) {
    fxContainer.classList.add('watts-strobe-active');
    
    if (isBassHeavy) {
        document.body.classList.add('watts-body-thump');
    }

    // Auto-clean burst after 1.5 seconds to avoid over-exposure
    clearTimeout(strobeTimeout);
    strobeTimeout = setTimeout(() => {
        fxContainer.classList.remove('watts-strobe-active');
        document.body.classList.remove('watts-body-thump');
    }, 1500);
}

// 2. SMOKESCREEN FOGGER
let fogLayer = null;
function adjustFogger(intensity) {
    if (!fogLayer) {
        fogLayer = document.createElement('div');
        fogLayer.className = 'watts-fog-overlay';
        fxContainer.appendChild(fogLayer);
    }
    
    // Scale opacity based on incoming control (max 0.15 for readability)
    const targetOpacity = intensity * 0.15;
    fogLayer.style.opacity = targetOpacity;
}

// 3. LASER SHOW EFFECT
function triggerLaserShow(color = '#00FFCC') {
    // Generate sweeping multi-tiered laser lines
    const laser = document.createElement('div');
    laser.className = 'watts-laser-line';
    laser.style.top = `${Math.random() * 80 + 10}%`;
    
    // Dynamic color injection: use the theme color with a fade-to-transparent gradient
    laser.style.background = `linear-gradient(90deg, transparent, ${color}, transparent)`;
    laser.style.boxShadow = `0 0 5px ${color}`;
    
    fxContainer.appendChild(laser);

    // Dynamic sweep animation trigger
    requestAnimationFrame(() => {
        laser.style.transform = `scaleY(2) rotate(${Math.random() * 4 - 2}deg)`;
    });

    // Remove entity from DOM once sweeping motion finishes
    setTimeout(() => {
        laser.remove();
    }, 1200);
}
})();