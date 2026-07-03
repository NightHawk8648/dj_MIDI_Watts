export async function syncAudioAnalysis(trackId, token) {
    if (!trackId || !token) return;

    try {
        const response = await fetch(`https://api.spotify.com/v1/audio-analysis/${trackId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            console.error("Failed to fetch audio analysis");
            return;
        }

        const analysisData = await response.json();
        
        // Map Spotify's Audio Analysis to the AI Equalizer and MIDI functions
        updateAIEqualizer(analysisData);
        syncMidiClock(analysisData.track.tempo);

    } catch (e) {
        console.error("Error syncing audio analysis:", e);
    }
}

function updateAIEqualizer(data) {
    // We can use data.segments to visualize Pitch and Timbre
    // For now, we will draw a basic representation on the new Spotify canvas
    const canvas = document.getElementById('spotify-eq-canvas');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    
    // Simplistic visualizer logic mapping pitch array to bars
    if (data.segments && data.segments.length > 0) {
        const currentSegment = data.segments[0]; // In a real app, this would sync with playback time
        const pitches = currentSegment.pitches; // 12-element array (chroma)

        const barWidth = canvas.width / pitches.length;
        for (let i = 0; i < pitches.length; i++) {
            const barHeight = pitches[i] * canvas.height;
            ctx.fillStyle = `hsl(${(i * 360) / 12}, 80%, 50%)`;
            ctx.fillRect(i * barWidth, canvas.height - barHeight, barWidth - 2, barHeight);
        }
    }
}

function syncMidiClock(tempo) {
    console.log(`[AI SYNC] Adjusting MIDI Clock to Spotify Tempo: ${tempo} BPM`);
    // Assuming a global dispatcher exists in the main script.js
    if (window.dispatchMidiClock) {
        window.dispatchMidiClock(tempo);
    }
}
