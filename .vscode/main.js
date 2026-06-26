import React, { useState, useEffect, useMemo, useRef } from 'react';
import { createRoot } from 'react-dom/client';
import { Sliders, Activity, Music, Share2, Video, Sparkles } from 'lucide-react';

const AudioController = () => {
    const [isInitialized, setIsInitialized] = useState(false);
    const [lowGain, setLowGain] = useState(0);
    const [midGain, setMidGain] = useState(0);
    const [highGain, setHighGain] = useState(0);
    const [reverbWet, setReverbWet] = useState(0); // 0.0 to 1.0
    const [delayTime, setDelayTime] = useState(0.3); // seconds
    const [delayFeedback, setDelayFeedback] = useState(0.4); // 0.0 to 1.0
    const [delayLowPass, setDelayLowPass] = useState(20000); // Hz
    const [delayHighPass, setDelayHighPass] = useState(20); // Hz

    const [trackData, setTrackData] = useState({ bpm: '--', key: '--', energy: '--', genre: 'cyberpunk techno' });
    const [aiThemeUrl, setAiThemeUrl] = useState(null);
    const [vjGif, setVjGif] = useState(null);
    const [isVjMode, setIsVjMode] = useState(false);
    const [beatActive, setBeatActive] = useState(false);
    const [midiDevices, setMidiDevices] = useState([]);

    const audioCtxRef = useRef(null);
    const eqLowRef = useRef(null);
    const eqMidRef = useRef(null);
    const eqHighRef = useRef(null);
    const compressorRef = useRef(null);
    const reverbNodeRef = useRef(null);
    const reverbWetGainRef = useRef(null);
    const delayNodeRef = useRef(null);
    const delayFeedbackGainRef = useRef(null);
    const delayLowPassFilterRef = useRef(null);
    const delayHighPassFilterRef = useRef(null);
    const analyserRef = useRef(null);
    const canvasRef = useRef(null);
    const sourceRef = useRef(null);
    const requestRef = useRef(null); // Ref to track animation frame for clean disposal

    // --- PRE-PERFORMANCE PREPARATION (Cloud Layer) ---
    const fetchReplicateVisual = async (genre) => {
        const REPLICATE_PROXY = 'https://api.replicate.com/v1/predictions'; // Usually proxied for security
        const API_TOKEN = 'YOUR_REPLICATE_API_TOKEN'; // Set this via environment/secure storage

        try {
            // 1. Start the prediction (Using a fast model like SDXL-Turbo or Lightning)
            const response = await fetch(REPLICATE_PROXY, {
                method: 'POST',
                headers: {
                    'Authorization': `Token ${API_TOKEN}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    version: "ac7327c201e7553b775897507c5a2344c8839603e57911efff972db91cd5e95e", // SDXL Lightning version
                    input: { prompt: `Abstract tech-noir VJ loop, ${genre} aesthetic, neon wires, deep shadows, 8k, futuristic digital art` }
                })
            });
            
            const prediction = await response.json();
            
            // 2. Poll for the result
            const pollResult = async (url) => {
                const res = await fetch(url, { headers: { 'Authorization': `Token ${API_TOKEN}` } });
                const data = await res.json();
                if (data.status === 'succeeded') {
                    setAiThemeUrl(data.output[0]);
                } else if (data.status !== 'failed') {
                    setTimeout(() => pollResult(url), 1000);
                }
            };

            if (prediction.urls && prediction.urls.get) {
                pollResult(prediction.urls.get);
            }
        } catch (err) {
            console.error("Replicate API Error:", err);
        }
    };

    useEffect(() => {
        if (isVjMode && trackData.genre !== '--') {
            fetchReplicateVisual(trackData.genre);
        }
    }, [isVjMode, trackData.genre]);

    // --- LIVE PERFORMANCE LOOP (Native Browser APIs) ---
    const render = () => {
        if (!analyserRef.current || !canvasRef.current) {
            requestRef.current = requestAnimationFrame(render);
            return;
        }

        const canvas = canvasRef.current;
        const ctx = canvas.getContext('2d');
        const bufferLength = analyserRef.current.frequencyBinCount;
        const dataArray = new Uint8Array(bufferLength);

        analyserRef.current.getByteFrequencyData(dataArray);
        
        // High-performance Beat Detection (Native Audio Data)
        // We only check the first few bins (sub-bass) to trigger visual pulses
        const avgBass = (dataArray[0] + dataArray[1] + dataArray[2]) / 3;
        if (avgBass > 210 && !beatActive) { 
            setBeatActive(true);
            setTimeout(() => setBeatActive(false), 50);
        }

        ctx.fillStyle = 'rgb(15, 23, 42)'; // Slate-900
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        const barWidth = (canvas.width / bufferLength) * 2.5;
        let x = 0;
        for (let i = 0; i < bufferLength; i++) {
            const barHeight = (dataArray[i] / 255) * canvas.height;
            ctx.fillStyle = `rgb(59, 130, 246)`; 
            ctx.fillRect(x, canvas.height - barHeight, barWidth, barHeight);
            x += barWidth + 1;
        }
        requestRef.current = requestAnimationFrame(render);
    };

    useEffect(() => {
        if (isInitialized) {
            requestRef.current = requestAnimationFrame(render);
        }
        return () => cancelAnimationFrame(requestRef.current);
    }, [isInitialized]);

    const initializeAudio = async () => {
        if (isInitialized) return;

        try {
            // 1. Initialize Audio Context (The Gateway to the DAC)
            audioCtxRef.current = new (window.AudioContext || window.webkitAudioContext)({ latencyHint: 'interactive' });
            
            // 2. Setup ADC (Analog-to-Digital Converter)
            const stream = await navigator.mediaDevices.getUserMedia({ 
                audio: {
                    echoCancellation: false,
                    noiseSuppression: false,
                    autoGainControl: false
                } 
            });
            
            sourceRef.current = audioCtxRef.current.createMediaStreamSource(stream);

            // 3. Setup Processing Nodes (3-Band EQ)
            eqLowRef.current = audioCtxRef.current.createBiquadFilter();
            eqLowRef.current.type = 'lowshelf';
            eqLowRef.current.frequency.value = 200;
            eqLowRef.current.gain.value = lowGain;

            eqMidRef.current = audioCtxRef.current.createBiquadFilter();
            eqMidRef.current.type = 'peaking';
            eqMidRef.current.frequency.value = 1000;
            eqMidRef.current.gain.value = midGain;

            eqHighRef.current = audioCtxRef.current.createBiquadFilter();
            eqHighRef.current.type = 'highshelf';
            eqHighRef.current.frequency.value = 5000;
            eqHighRef.current.gain.value = highGain;

            // 4. Setup Reverb (ConvolverNode)
            reverbNodeRef.current = audioCtxRef.current.createConvolver();
            reverbWetGainRef.current = audioCtxRef.current.createGain();
            reverbWetGainRef.current.gain.value = reverbWet;

            // Load an impulse response for the reverb
            // For a real application, you'd fetch a small WAV file (e.g., from a public domain library)
            // and decode it into an AudioBuffer. For this example, we'll use a placeholder.
            // Example: const impulseResponseUrl = 'path/to/your/small_impulse_response.wav';
            // fetch(impulseResponseUrl)
            //     .then(response => response.arrayBuffer())
            //     .then(buffer => audioCtxRef.current.decodeAudioData(buffer))
            //     .then(decodedBuffer => {
            //         reverbNodeRef.current.buffer = decodedBuffer;
            //     })
            //     .catch(e => console.error("Error loading impulse response:", e));
            // For now, reverb will be silent until a buffer is assigned.

            // 5. Setup Delay (DelayNode with feedback and filters)
            delayNodeRef.current = audioCtxRef.current.createDelay(5.0); // Max delay of 5 seconds
            delayNodeRef.current.delayTime.value = delayTime;

            delayFeedbackGainRef.current = audioCtxRef.current.createGain();
            delayFeedbackGainRef.current.gain.value = delayFeedback;

            delayLowPassFilterRef.current = audioCtxRef.current.createBiquadFilter();
            delayLowPassFilterRef.current.type = 'lowpass';
            delayLowPassFilterRef.current.frequency.value = delayLowPass;

            delayHighPassFilterRef.current = audioCtxRef.current.createBiquadFilter();
            delayHighPassFilterRef.current.type = 'highpass';
            delayHighPassFilterRef.current.frequency.value = delayHighPass;

            // 4. Setup Dynamics Compressor (for vocal consistency/limiting)
            compressorRef.current = audioCtxRef.current.createDynamicsCompressor();
            compressorRef.current.threshold.setValueAtTime(-24, audioCtxRef.current.currentTime);
            compressorRef.current.knee.setValueAtTime(30, audioCtxRef.current.currentTime);
            compressorRef.current.ratio.setValueAtTime(12, audioCtxRef.current.currentTime);
            compressorRef.current.attack.setValueAtTime(0.003, audioCtxRef.current.currentTime);
            compressorRef.current.release.setValueAtTime(0.25, audioCtxRef.current.currentTime);

            // 7. Master Gain (for overall output level, not yet implemented but good to plan for)
            // masterGainRef.current = audioCtxRef.current.createGain();
            // masterGainRef.current.gain.value = 1.0;

            // 5. Setup Analyser for Visualizer
            analyserRef.current = audioCtxRef.current.createAnalyser();
            analyserRef.current.fftSize = 256;

            // 6. Connect the Graph: ADC -> EQ Chain -> Compressor -> Analyser -> DAC
            sourceRef.current.connect(eqLowRef.current);
            eqLowRef.current.connect(eqMidRef.current);
            eqMidRef.current.connect(eqHighRef.current);
            eqHighRef.current.connect(compressorRef.current);

            // Connect Reverb in parallel (wet signal)
            compressorRef.current.connect(reverbNodeRef.current);
            reverbNodeRef.current.connect(reverbWetGainRef.current);
            reverbWetGainRef.current.connect(analyserRef.current); // Connect wet reverb to analyser

            // Connect Delay in series after compressor (or parallel, depending on desired effect)
            // For this example, we'll put it after reverb for a more complex sound.
            compressorRef.current.connect(delayNodeRef.current); // Dry signal to delay
            delayNodeRef.current.connect(delayLowPassFilterRef.current); // Delay output to LPF
            delayLowPassFilterRef.current.connect(delayHighPassFilterRef.current); // LPF to HPF
            delayHighPassFilterRef.current.connect(delayFeedbackGainRef.current); // Filtered delay to feedback gain
            delayFeedbackGainRef.current.connect(delayNodeRef.current); // Feedback loop
            delayFeedbackGainRef.current.connect(analyserRef.current); // Connect delayed signal to analyser
            analyserRef.current.connect(audioCtxRef.current.destination);

            if (audioCtxRef.current.state === 'suspended') {
                await audioCtxRef.current.resume();
            }
            
            setIsInitialized(true);
        } catch (err) {
            console.error("Audio initialization failed:", err);
            alert("Microphone access is required for the ADC to function.");
        }
    };

    // Web MIDI API Integration
    useEffect(() => {
        if (!navigator.requestMIDIAccess) return;

        navigator.requestMIDIAccess().then((access) => {
            const inputs = Array.from(access.inputs.values());
            setMidiDevices(inputs.map(i => i.name));

            inputs.forEach(input => {
                input.onmidimessage = (message) => {
                    const [status, data1, data2] = message.data;
                    if (status === 176) { // Control Change
                        const gainValue = ((data2 / 127) * 48) - 24;
                        const rounded = parseFloat(gainValue.toFixed(1));
                        
                        switch(data1) {
                            case 1: // Knob 1 -> Low
                                setLowGain(rounded);
                                break;
                            case 2: // Knob 2 -> Mid
                                setMidGain(rounded);
                                break;
                            case 3: // Knob 3 -> High
                                setHighGain(rounded);
                                break;
                            case 4: // Knob 4 -> Reverb Wet
                                setReverbWet(parseFloat((data2 / 127).toFixed(2))); // 0.0 to 1.0
                                break;
                            case 5: // Knob 5 -> Delay Time
                                setDelayTime(parseFloat(((data2 / 127) * 2).toFixed(2))); // 0 to 2 seconds
                                break;
                            case 6: // Knob 6 -> Delay Feedback
                                setDelayFeedback(parseFloat(((data2 / 127) * 0.9).toFixed(2))); // 0 to 0.9 for stable feedback
                                break;
                            case 7: // Knob 7 -> Delay Low Pass Filter
                                // Map 0-127 to 500Hz-20000Hz (logarithmic scale is better for frequency)
                                const lpFreq = 500 + (data2 / 127) * 19500;
                                setDelayLowPass(parseFloat(lpFreq.toFixed(0)));
                                break;
                            case 8: // Knob 8 -> Delay High Pass Filter
                                // Map 0-127 to 20Hz-1000Hz
                                const hpFreq = 20 + (data2 / 127) * 980;
                                setDelayHighPass(parseFloat(hpFreq.toFixed(0)));
                                break;

                        }
                    }
                };
            });

            access.onstatechange = (e) => {
                setMidiDevices(Array.from(access.inputs.values()).map(i => i.name));
            };
        });
    }, []);

    // Update Audio Nodes when React State changes
    useEffect(() => {
        if (!audioCtxRef.current) return;

        // EQ
        if (eqLowRef.current) eqLowRef.current.gain.value = lowGain; // dB
        if (eqMidRef.current) eqMidRef.current.gain.value = midGain; // dB
        if (eqHighRef.current) eqHighRef.current.gain.value = highGain; // dB

        // Reverb
        if (reverbWetGainRef.current) reverbWetGainRef.current.gain.value = reverbWet; // Linear gain

        // Delay
        if (delayNodeRef.current) delayNodeRef.current.delayTime.value = delayTime; // Seconds
        if (delayFeedbackGainRef.current) delayFeedbackGainRef.current.gain.value = delayFeedback; // Linear gain
        if (delayLowPassFilterRef.current) delayLowPassFilterRef.current.frequency.value = delayLowPass; // Hz
        if (delayHighPassFilterRef.current) delayHighPassFilterRef.current.frequency.value = delayHighPass; // Hz
    }, [lowGain, midGain, highGain, reverbWet, delayTime, delayFeedback, delayLowPass, delayHighPass]);

    return (
        <div className="p-4 bg-slate-900 text-white min-h-screen">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-xl font-bold">dj_MIDI_Watts</h1>
                <div className="flex gap-2">
                    <div className="flex items-center gap-2 px-3 py-1 bg-slate-800 rounded-full text-xs">
                        <Activity size={14} className={midiDevices.length ? "text-green-400" : "text-slate-500"} />
                        {midiDevices.length ? `${midiDevices.length} MIDI Device(s)` : "No MIDI detected"}
                    </div>
                    {!isInitialized && (
                        <button 
                            onClick={initializeAudio}
                            className="px-4 py-1 bg-blue-600 hover:bg-blue-500 rounded font-bold text-sm transition-colors"
                        >
                            START ENGINE
                        </button>
                    )}
                </div>
            </div>
            
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Animated Display (Visualizer) */}
                <div className="lg:col-span-2 relative bg-slate-950 rounded-xl overflow-hidden border border-slate-800 h-80 shadow-inner">
                    <canvas ref={canvasRef} className="w-full h-full" width={800} height={256} />
                    
                    {/* VJ Overlay (Replicate/AI Integration) */}
                    {isVjMode && (aiThemeUrl || vjGif) && (
                        <div 
                            className={`absolute inset-0 flex items-center justify-center transition-all duration-75 pointer-events-none ${beatActive ? 'scale-110 opacity-80' : 'scale-100 opacity-40'}`}
                            style={{ 
                                filter: `hue-rotate(${highGain * 5}deg) saturate(${100 + midGain * 2}%) brightness(${100 + lowGain}%)` 
                            }}
                        >
                            <img src={aiThemeUrl || vjGif} alt="AI Visual" className="w-full h-full object-cover mix-blend-screen" />
                        </div>
                    )}
                </div>

                {/* Cloud Analysis Panel (Spotify/Deezer Stubs) */}
                <div className="panel bg-slate-800 p-4 rounded-xl border border-slate-700 shadow-xl">
                    <div className="flex items-center gap-2 mb-4 text-purple-400">
                        <Music size={18} />
                        <span className="font-semibold text-sm uppercase tracking-wider">Cloud Insights</span>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                        <div className="bg-slate-900 p-2 rounded border border-slate-700">
                            <div className="text-[10px] text-slate-500">BPM</div>
                            <div className="text-lg font-mono text-purple-300">{trackData.bpm}</div>
                        </div>
                        <div className="bg-slate-900 p-2 rounded border border-slate-700">
                            <div className="text-[10px] text-slate-500">KEY</div>
                            <div className="text-lg font-mono text-purple-300">{trackData.key}</div>
                        </div>
                    </div>
                    
                    {/* VJ Controls */}
                    <div className="mt-4 pt-4 border-t border-slate-700">
                        <button 
                            onClick={() => setIsVjMode(!isVjMode)}
                            className={`w-full flex items-center justify-center gap-2 py-2 rounded font-bold text-xs transition-all ${isVjMode ? 'bg-purple-600 text-white' : 'bg-slate-700 text-slate-400'}`}
                        >
                            <Video size={14} />
                            {isVjMode ? "VJ MODE: ACTIVE" : "ENABLE VJ MODE"}
                        </button>
                        <div className="text-[10px] text-slate-500 mt-2 text-center uppercase tracking-tighter italic">AI Morphing via MIDI CC 1-3</div>
                    </div>
                </div>

                {/* Audio Effects Panel */}
                <div className="panel bg-slate-800 p-4 rounded-xl border border-slate-700 shadow-xl">
                    <div className="flex items-center gap-2 mb-4 text-green-400">
                        <Sparkles size={18} />
                        <span className="font-semibold text-sm uppercase tracking-wider">Audio Effects</span>
                    </div>
                    
                    <div className="space-y-6">
                        {/* Reverb */}
                        <div>
                            <div className="flex justify-between mb-1"><label className="text-xs text-slate-400 uppercase">Reverb Wet</label><span className="text-xs font-mono">{(reverbWet * 100).toFixed(0)}%</span></div>
                            <input 
                                type="range" min="0" max="1" step="0.01" value={reverbWet} 
                                disabled={!isInitialized}
                                onChange={(e) => setReverbWet(parseFloat(e.target.value))}
                                className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-green-500 disabled:opacity-50"
                            />
                            <p className="text-[10px] text-slate-500 mt-1">
                                *Requires impulse response (e.g., `impulse.wav`) to be loaded.
                            </p>
                        </div>

                        {/* Delay */}
                        <div className="pt-4 border-t border-slate-700">
                            <h3 className="text-sm font-semibold text-green-300 mb-3">Delay</h3>
                            <div>
                                <div className="flex justify-between mb-1"><label className="text-xs text-slate-400 uppercase">Time</label><span className="text-xs font-mono">{delayTime.toFixed(2)}s</span></div>
                                <input 
                                    type="range" min="0" max="2" step="0.01" value={delayTime} 
                                    disabled={!isInitialized}
                                    onChange={(e) => setDelayTime(parseFloat(e.target.value))}
                                    className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-green-400 disabled:opacity-50"
                                />
                            </div>
                            <div className="mt-4">
                                <div className="flex justify-between mb-1"><label className="text-xs text-slate-400 uppercase">Feedback</label><span className="text-xs font-mono">{(delayFeedback * 100).toFixed(0)}%</span></div>
                                <input 
                                    type="range" min="0" max="0.9" step="0.01" value={delayFeedback} 
                                    disabled={!isInitialized}
                                    onChange={(e) => setDelayFeedback(parseFloat(e.target.value))}
                                    className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-green-400 disabled:opacity-50"
                                />
                            </div>
                            <div className="mt-4">
                                <div className="flex justify-between mb-1"><label className="text-xs text-slate-400 uppercase">Low Pass</label><span className="text-xs font-mono">{delayLowPass.toFixed(0)}Hz</span></div>
                                <input 
                                    type="range" min="500" max="20000" step="100" value={delayLowPass} 
                                    disabled={!isInitialized}
                                    onChange={(e) => setDelayLowPass(parseFloat(e.target.value))}
                                    className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-green-300 disabled:opacity-50"
                                />
                            </div>
                            <div className="mt-4">
                                <div className="flex justify-between mb-1"><label className="text-xs text-slate-400 uppercase">High Pass</label><span className="text-xs font-mono">{delayHighPass.toFixed(0)}Hz</span></div>
                                <input 
                                    type="range" min="20" max="1000" step="10" value={delayHighPass} 
                                    disabled={!isInitialized}
                                    onChange={(e) => setDelayHighPass(parseFloat(e.target.value))}
                                    className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-green-300 disabled:opacity-50"
                                />
                            </div>
                        </div>

                        {/* Placeholder for other effects */}
                        <div className="pt-4 border-t border-slate-700">
                            <h3 className="text-sm font-semibold text-yellow-300 mb-3">Other Effects</h3>
                            <div className="text-xs text-slate-500 italic">
                                Distortion, Saturation, Compression, Chorus, Flanger, Phaser, Noise Gate, Limiter, Pitch Modifier...
                            </div>
                            <div className="mt-2">
                                <label className="block mb-1 text-xs text-slate-400 uppercase">Distortion</label>
                                <input type="range" min="0" max="1" step="0.01" value="0" disabled={true} className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer disabled:opacity-50" />
                            </div>
                            <div className="mt-2">
                                <label className="block mb-1 text-xs text-slate-400 uppercase">Compression</label>
                                <input type="range" min="0" max="1" step="0.01" value="0" disabled={true} className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer disabled:opacity-50" />
                            </div>
                        </div>
                    </div>
                </div>

                {/* EQ Controls */}
                <div className="panel bg-slate-800 p-4 rounded-xl border border-slate-700 shadow-xl">
                    <div className="flex items-center gap-2 mb-4 text-blue-400">
                        <Sliders size={18} />
                        <span className="font-semibold text-sm uppercase tracking-wider">3-Band Equalizer</span>
                    </div>
                    
                    <div className="space-y-6">
                        <div>
                            <div className="flex justify-between mb-1"><label className="text-xs text-slate-400 uppercase">Low</label><span className="text-xs font-mono">{lowGain}dB</span></div>
                            <input 
                                type="range" min="-24" max="24" step="0.5" value={lowGain} 
                                disabled={!isInitialized}
                                onChange={(e) => setLowGain(parseFloat(e.target.value))}
                                className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-blue-500 disabled:opacity-50"
                            />
                        </div>
                        <div>
                            <div className="flex justify-between mb-1"><label className="text-xs text-slate-400 uppercase">Mid</label><span className="text-xs font-mono">{midGain}dB</span></div>
                            <input 
                                type="range" min="-24" max="24" step="0.5" value={midGain} 
                                disabled={!isInitialized}
                                onChange={(e) => setMidGain(parseFloat(e.target.value))}
                                className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-blue-400 disabled:opacity-50"
                            />
                        </div>
                        <div>
                            <div className="flex justify-between mb-1"><label className="text-xs text-slate-400 uppercase">High</label><span className="text-xs font-mono">{highGain}dB</span></div>
                            <input 
                                type="range" min="-24" max="24" step="0.5" value={highGain} 
                                disabled={!isInitialized}
                                onChange={(e) => setHighGain(parseFloat(e.target.value))}
                                className="w-full h-2 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-blue-300 disabled:opacity-50"
                            />
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

createRoot(document.getElementById('root')).render(<AudioController />);