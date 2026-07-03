import { getSpotifyToken } from './spotify-auth.js';
import { syncAudioAnalysis } from './spotify-ai-sync.js';

export function initializeSpotifyPlayer() {
    window.onSpotifyWebPlaybackSDKReady = () => {
        const token = getSpotifyToken();
        if (!token) return;

        const player = new Spotify.Player({
            name: 'DJ MIDI Watts Web Player',
            getOAuthToken: cb => { cb(token); },
            volume: 0.5
        });

        // Ready
        player.addListener('ready', ({ device_id }) => {
            console.log('Ready with Device ID', device_id);
            document.getElementById('spotify-status').innerText = "Spotify Ready (Premium Required)";
        });

        // Not Ready
        player.addListener('not_ready', ({ device_id }) => {
            console.log('Device ID has gone offline', device_id);
        });

        player.addListener('initialization_error', ({ message }) => {
            console.error(message);
        });

        player.addListener('authentication_error', ({ message }) => {
            console.error(message);
        });

        player.addListener('account_error', ({ message }) => {
            console.error("Account Error: Premium is required for Web Playback SDK.", message);
            document.getElementById('spotify-status').innerText = "Account Error: Premium Required";
        });

        // Playback status updates
        player.addListener('player_state_changed', state => {
            if (!state) return;

            const currentTrack = state.track_window.current_track;
            if (currentTrack) {
                document.getElementById('spotify-track-name').innerText = currentTrack.name;
                document.getElementById('spotify-artist-name').innerText = currentTrack.artists.map(a => a.name).join(', ');
                if (currentTrack.album.images.length > 0) {
                    document.getElementById('spotify-album-art').src = currentTrack.album.images[0].url;
                }

                // Fire AI Sync with the new track ID
                syncAudioAnalysis(currentTrack.id, token);
            }
        });

        player.connect();
    };
}
