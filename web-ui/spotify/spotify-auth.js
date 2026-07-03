const SPOTIFY_CLIENT_ID = "YOUR_SPOTIFY_CLIENT_ID";
const SPOTIFY_REDIRECT_URI = "http://localhost:8080";

// PKCE Authorization Code Flow
async function generateCodeChallenge(codeVerifier) {
    const data = new TextEncoder().encode(codeVerifier);
    const digest = await window.crypto.subtle.digest('SHA-256', data);
    return btoa(String.fromCharCode.apply(null, [...new Uint8Array(digest)]))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');
}

function generateRandomString(length) {
    let text = '';
    let possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    for (let i = 0; i < length; i++) {
        text += possible.charAt(Math.floor(Math.random() * possible.length));
    }
    return text;
}

export async function loginWithSpotify() {
    const codeVerifier = generateRandomString(128);
    const codeChallenge = await generateCodeChallenge(codeVerifier);
    
    localStorage.setItem('spotify_code_verifier', codeVerifier);
    
    const scope = 'streaming user-read-email user-read-private user-read-playback-state user-modify-playback-state';
    
    const authUrl = new URL("https://accounts.spotify.com/authorize");
    authUrl.search = new URLSearchParams({
        response_type: 'code',
        client_id: SPOTIFY_CLIENT_ID,
        scope: scope,
        redirect_uri: SPOTIFY_REDIRECT_URI,
        code_challenge_method: 'S256',
        code_challenge: codeChallenge
    }).toString();
    
    window.location.href = authUrl.toString();
}

export async function handleSpotifyCallback(code) {
    const codeVerifier = localStorage.getItem('spotify_code_verifier');
    
    const payload = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
            client_id: SPOTIFY_CLIENT_ID,
            grant_type: 'authorization_code',
            code: code,
            redirect_uri: SPOTIFY_REDIRECT_URI,
            code_verifier: codeVerifier
        })
    };
    
    try {
        const body = await fetch("https://accounts.spotify.com/api/token", payload);
        const response = await body.json();
        
        if (response.access_token) {
            localStorage.setItem('spotify_access_token', response.access_token);
            localStorage.setItem('spotify_refresh_token', response.refresh_token);
            return response.access_token;
        }
    } catch (e) {
        console.error("Spotify Auth Error", e);
    }
    return null;
}

export function getSpotifyToken() {
    return localStorage.getItem('spotify_access_token');
}
