(function () {
    "use strict";

    const WATCH_PROGRESS_PREFIX = "media-watch-progress:";
    const SAVE_INTERVAL_SECONDS = 5;
    const players = new Map();
    const progressTrackers = new Map();

    function readProgress(storageKey) {
        try {
            const savedTime = Number.parseFloat(localStorage.getItem(storageKey));
            return Number.isFinite(savedTime) && savedTime > 0
                ? savedTime
                : null;
        } catch (_error) {
            return null;
        }
    }

    function writeProgress(storageKey, currentTime) {
        try {
            localStorage.setItem(storageKey, String(currentTime));
        } catch (_error) {
            // Playback remains available when browser storage is disabled.
        }
    }

    function removeProgress(storageKey) {
        try {
            localStorage.removeItem(storageKey);
        } catch (_error) {
            // Playback remains available when browser storage is disabled.
        }
    }

    function initializeWatchProgress(mediaElement) {
        if (!(mediaElement instanceof HTMLVideoElement)
                || progressTrackers.has(mediaElement)) {
            return;
        }

        const mediaId = mediaElement.dataset.mediaProgressId;
        if (!mediaId) {
            return;
        }

        const storageKey = WATCH_PROGRESS_PREFIX + mediaId;
        let lastSavedSecond = -SAVE_INTERVAL_SECONDS;

        function restoreProgress() {
            const savedTime = readProgress(storageKey);
            if (savedTime == null) {
                return;
            }

            const nearCompletion = Number.isFinite(mediaElement.duration)
                    && mediaElement.duration - savedTime <= SAVE_INTERVAL_SECONDS;
            if (nearCompletion) {
                removeProgress(storageKey);
                return;
            }

            mediaElement.currentTime = savedTime;
            lastSavedSecond = Math.floor(savedTime);
        }

        function saveProgress(force) {
            const currentSecond = Math.floor(mediaElement.currentTime);
            if (!Number.isFinite(currentSecond) || currentSecond <= 0) {
                return;
            }

            const completed = mediaElement.ended
                    || Number.isFinite(mediaElement.duration)
                    && mediaElement.duration - currentSecond <= SAVE_INTERVAL_SECONDS;
            if (completed) {
                removeProgress(storageKey);
                return;
            }

            if (!force
                    && Math.abs(currentSecond - lastSavedSecond) < SAVE_INTERVAL_SECONDS) {
                return;
            }

            writeProgress(storageKey, currentSecond);
            lastSavedSecond = currentSecond;
        }

        function handleTimeUpdate() {
            saveProgress(false);
        }

        function handlePause() {
            saveProgress(true);
        }

        function handleEnded() {
            removeProgress(storageKey);
        }

        mediaElement.addEventListener("loadedmetadata", restoreProgress);
        mediaElement.addEventListener("timeupdate", handleTimeUpdate);
        mediaElement.addEventListener("pause", handlePause);
        mediaElement.addEventListener("ended", handleEnded);

        progressTrackers.set(mediaElement, {
            save: () => saveProgress(true),
            destroy: function () {
                mediaElement.removeEventListener("loadedmetadata", restoreProgress);
                mediaElement.removeEventListener("timeupdate", handleTimeUpdate);
                mediaElement.removeEventListener("pause", handlePause);
                mediaElement.removeEventListener("ended", handleEnded);
            }
        });
    }

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

        initializeWatchProgress(mediaElement);

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
        const progressTracker = progressTrackers.get(mediaElement);
        if (progressTracker) {
            progressTracker.save();
            progressTracker.destroy();
            progressTrackers.delete(mediaElement);
        }

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

    window.addEventListener("pagehide", function () {
        progressTrackers.forEach(function (progressTracker) {
            progressTracker.save();
        });
    });

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
