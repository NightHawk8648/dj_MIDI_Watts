const YOUTUBE_CLIENT_ID = "YOUR_YOUTUBE_CLIENT_ID";
const YOUTUBE_REDIRECT_URI = "http://localhost:8080";

export function loginWithYouTube() {
    const scope = 'https://www.googleapis.com/auth/youtube.readonly';
    
    const authUrl = new URL("https://accounts.google.com/o/oauth2/v2/auth");
    authUrl.search = new URLSearchParams({
        client_id: YOUTUBE_CLIENT_ID,
        redirect_uri: YOUTUBE_REDIRECT_URI,
        response_type: 'token',
        scope: scope,
        include_granted_scopes: 'true',
        state: 'youtube_auth'
    }).toString();
    
    window.location.href = authUrl.toString();
}

export function handleYouTubeCallback() {
    const hash = window.location.hash.substring(1);
    const params = new URLSearchParams(hash);
    
    const accessToken = params.get('access_token');
    const state = params.get('state');
    
    if (accessToken && state === 'youtube_auth') {
        localStorage.setItem('youtube_access_token', accessToken);
        // Clear hash
        window.history.replaceState({}, document.title, window.location.pathname);
        return accessToken;
    }
    return null;
}

export function getYouTubeToken() {
    return localStorage.getItem('youtube_access_token');
}
