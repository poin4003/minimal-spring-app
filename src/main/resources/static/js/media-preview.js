(function () {
    "use strict";

    const players = new Map();

    function showPlaybackError(mediaElement, message) {
        const errorElement = mediaElement.parentElement.querySelector("[data-hls-error]");
        if (!errorElement) {
            return;
        }

        errorElement.textContent = message;
        errorElement.hidden = false;
    }

    function initializePlayer(mediaElement) {
        if (players.has(mediaElement)) {
            return;
        }

        const sourceUrl = mediaElement.dataset.hlsSource;
        if (!sourceUrl) {
            showPlaybackError(mediaElement, "No stream source is available.");
            return;
        }

        if (mediaElement.canPlayType("application/vnd.apple.mpegurl")) {
            mediaElement.src = sourceUrl;
            players.set(mediaElement, null);
            return;
        }

        if (!window.Hls || !window.Hls.isSupported()) {
            showPlaybackError(mediaElement, "HLS playback is not supported by this browser.");
            return;
        }

        const hls = new window.Hls();
        hls.loadSource(sourceUrl);
        hls.attachMedia(mediaElement);
        hls.on(window.Hls.Events.ERROR, function (_event, data) {
            if (!data.fatal) {
                return;
            }

            if (data.type === window.Hls.ErrorTypes.NETWORK_ERROR) {
                hls.startLoad();
                return;
            }
            if (data.type === window.Hls.ErrorTypes.MEDIA_ERROR) {
                hls.recoverMediaError();
                return;
            }

            hls.destroy();
            players.delete(mediaElement);
            showPlaybackError(mediaElement, "The stream could not be played.");
        });
        players.set(mediaElement, hls);
    }

    function destroyPlayer(mediaElement) {
        if (!players.has(mediaElement)) {
            return;
        }

        const hls = players.get(mediaElement);
        if (hls) {
            hls.destroy();
        }
        mediaElement.pause();
        mediaElement.removeAttribute("src");
        mediaElement.load();
        players.delete(mediaElement);
    }

    document.querySelectorAll("[data-media-preview-modal]").forEach(function (modalElement) {
        modalElement.addEventListener("shown.bs.modal", function () {
            modalElement.querySelectorAll("[data-hls-player]").forEach(function (mediaElement) {
                initializePlayer(mediaElement);
            });
        });

        modalElement.addEventListener("hidden.bs.modal", function () {
            modalElement.querySelectorAll("[data-hls-player]").forEach(function (mediaElement) {
                destroyPlayer(mediaElement);
            });
        });
    });
})();
