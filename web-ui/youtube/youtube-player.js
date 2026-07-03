let ytPlayer;

export function initializeYouTubePlayer() {
    // The IFrame API will call onYouTubeIframeAPIReady when it's loaded
    // We attach it to the window object.
    window.onYouTubeIframeAPIReady = () => {
        ytPlayer = new YT.Player('yt-player-container', {
            height: '200',
            width: '100%',
            videoId: '', // Start empty
            playerVars: {
                'playsinline': 1,
                'controls': 1,
                'autoplay': 0
            },
            events: {
                'onReady': onPlayerReady,
                'onStateChange': onPlayerStateChange
            }
        });
    };
}

function onPlayerReady(event) {
    document.getElementById('youtube-status').innerText = "YouTube Ready";
    console.log("YouTube Player is ready");
}

function onPlayerStateChange(event) {
    // 1 = playing, 2 = paused
    if (event.data == YT.PlayerState.PLAYING) {
        // Attempt to sync MIDI / Visualizer if possible
        const videoData = ytPlayer.getVideoData();
        if (videoData) {
            document.getElementById('youtube-track-name').innerText = videoData.title;
            document.getElementById('youtube-artist-name').innerText = videoData.author;
            console.log(`Now Playing YouTube: ${videoData.title}`);
        }
    }
}

export function loadYouTubeVideo(videoId) {
    if (ytPlayer && videoId) {
        ytPlayer.loadVideoById(videoId);
    }
}
