(function () {
    "use strict";

    const WATCH_PROGRESS_PREFIX = "media-watch-progress:";
    const SAVE_INTERVAL_SECONDS = 5;
    const playerSessions = new Map();
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

    function initializeWatchProgress(mediaElement, player) {
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

            const duration = player.duration();
            const nearCompletion = Number.isFinite(duration)
                    && duration - savedTime <= SAVE_INTERVAL_SECONDS;
            if (nearCompletion) {
                removeProgress(storageKey);
                return;
            }

            player.currentTime(savedTime);
            lastSavedSecond = Math.floor(savedTime);
        }

        function saveProgress(force) {
            const currentSecond = Math.floor(player.currentTime());
            if (!Number.isFinite(currentSecond) || currentSecond <= 0) {
                return;
            }

            const duration = player.duration();
            const completed = player.ended()
                    || Number.isFinite(duration)
                    && duration - currentSecond <= SAVE_INTERVAL_SECONDS;
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

        player.one("loadedmetadata", restoreProgress);
        player.on("timeupdate", handleTimeUpdate);
        player.on("pause", handlePause);
        player.on("ended", handleEnded);

        progressTrackers.set(mediaElement, {
            save: function () {
                saveProgress(true);
            },
            destroy: function () {
                player.off("timeupdate", handleTimeUpdate);
                player.off("pause", handlePause);
                player.off("ended", handleEnded);
            }
        });
    }

    function showPlaybackError(mediaElement, messageKey) {
        const previewElement = mediaElement.closest("[data-media-preview-player]");
        const errorElement = previewElement?.querySelector("[data-video-error]");
        const modalElement = mediaElement.closest("[data-media-preview-modal]");
        if (!errorElement) {
            return;
        }

        errorElement.textContent = modalElement?.dataset[messageKey] || "";
        errorElement.hidden = false;
    }

    function createPlayerOptions(mediaElement) {
        const isVideo = mediaElement instanceof HTMLVideoElement;
        const options = {
            controls: true,
            preload: "metadata",
            responsive: true,
            fluid: isVideo,
            audioOnlyMode: !isVideo,
            playbackRates: [0.5, 0.75, 1, 1.25, 1.5, 1.75, 2],
            html5: {
                vhs: {
                    overrideNative: true
                },
                nativeAudioTracks: false,
                nativeVideoTracks: false
            }
        };

        if (isVideo) {
            options.controlBar = {
                skipButtons: {
                    backward: 10,
                    forward: 10
                },
                pictureInPictureToggle: true,
                fullscreenToggle: true
            };
        }

        return options;
    }

    function initializePlayer(mediaElement) {
        if (playerSessions.has(mediaElement)) {
            return;
        }

        const sourceUrl = mediaElement.dataset.videoSource;
        if (!sourceUrl) {
            showPlaybackError(mediaElement, "messageSourceUnavailable");
            return;
        }

        if (!window.videojs) {
            showPlaybackError(mediaElement, "messagePlayerUnavailable");
            return;
        }

        const player = window.videojs(mediaElement, createPlayerOptions(mediaElement));
        playerSessions.set(mediaElement, {
            player: player
        });

        player.ready(function () {
            if (mediaElement instanceof HTMLVideoElement
                    && typeof player.hlsQualitySelector === "function") {
                player.hlsQualitySelector({
                    displayCurrentQuality: true
                });
            }

            initializeWatchProgress(mediaElement, player);
            player.src({
                src: sourceUrl,
                type: "application/x-mpegURL"
            });
        });

        player.on("error", function () {
            showPlaybackError(mediaElement, "messageStreamFailed");
        });
    }

    function destroyPlayer(mediaElement) {
        const progressTracker = progressTrackers.get(mediaElement);
        if (progressTracker) {
            progressTracker.save();
            progressTracker.destroy();
            progressTrackers.delete(mediaElement);
        }

        const session = playerSessions.get(mediaElement);
        if (session?.player && !session.player.isDisposed()) {
            session.player.dispose();
        }
        playerSessions.delete(mediaElement);
    }

    window.addEventListener("pagehide", function () {
        progressTrackers.forEach(function (progressTracker) {
            progressTracker.save();
        });
    });

    document.addEventListener("shown.bs.modal", function (event) {
        const modalElement = event.target;
        if (!(modalElement instanceof Element)
                || !modalElement.matches("[data-media-preview-modal]")) {
            return;
        }

        modalElement.querySelectorAll("[data-video-player]").forEach(function (mediaElement) {
            initializePlayer(mediaElement);
        });
    });

    document.addEventListener("hidden.bs.modal", function (event) {
        const modalElement = event.target;
        if (!(modalElement instanceof Element)
                || !modalElement.matches("[data-media-preview-modal]")) {
            return;
        }

        modalElement.querySelectorAll("[data-video-player]").forEach(function (mediaElement) {
            destroyPlayer(mediaElement);
        });
    });
})();
