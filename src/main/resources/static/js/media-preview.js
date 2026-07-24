(function () {
    "use strict";

    const WATCH_PROGRESS_PREFIX = "media-watch-progress:";
    const SAVE_INTERVAL_SECONDS = 5;
    const PLYR_ICON_URL = "/vendor/plyr/plyr.svg";
    const PLYR_BLANK_VIDEO_URL = "/vendor/plyr/blank.mp4";
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
            save: function () {
                saveProgress(true);
            },
            destroy: function () {
                mediaElement.removeEventListener("loadedmetadata", restoreProgress);
                mediaElement.removeEventListener("timeupdate", handleTimeUpdate);
                mediaElement.removeEventListener("pause", handlePause);
                mediaElement.removeEventListener("ended", handleEnded);
            }
        });
    }

    function showPlaybackError(mediaElement, message) {
        const previewElement = mediaElement.closest("[data-media-preview-player]");
        const errorElement = previewElement?.querySelector("[data-hls-error]");
        if (!errorElement) {
            return;
        }

        errorElement.textContent = message;
        errorElement.hidden = false;
    }

    function resolveLevelHeight(level) {
        return Number.isFinite(level.height) && level.height > 0
            ? level.height
            : null;
    }

    function resolveQualityOptions(hls) {
        const heights = hls.levels
            .map(function (level) {
                return resolveLevelHeight(level);
            })
            .filter(function (height) {
                return Number.isFinite(height) && height > 0;
            });

        return [
            0,
            ...new Set(heights)
        ].sort(function (left, right) {
            if (left === 0) {
                return -1;
            }
            if (right === 0) {
                return 1;
            }
            return right - left;
        });
    }

    function changeHlsQuality(hls, quality) {
        const height = Number(quality);
        if (height === 0) {
            hls.currentLevel = -1;
            return;
        }

        const levelIndex = hls.levels.findIndex(function (level) {
            return resolveLevelHeight(level) === height;
        });
        if (levelIndex >= 0) {
            hls.currentLevel = levelIndex;
        }
    }

    function createVideoPlayer(mediaElement, hls) {
        if (!window.Plyr) {
            return null;
        }

        const options = {
            controls: [
                "play-large",
                "play",
                "rewind",
                "fast-forward",
                "progress",
                "current-time",
                "duration",
                "mute",
                "volume",
                "settings",
                "pip",
                "fullscreen"
            ],
            settings: hls ? ["quality", "speed"] : ["speed"],
            seekTime: 10,
            iconUrl: PLYR_ICON_URL,
            blankVideo: PLYR_BLANK_VIDEO_URL,
            storage: {
                enabled: true,
                key: "media-player"
            },
            speed: {
                selected: 1,
                options: [0.5, 0.75, 1, 1.25, 1.5, 1.75, 2]
            }
        };

        if (hls) {
            options.quality = {
                default: 0,
                options: resolveQualityOptions(hls),
                forced: true,
                onChange: function (quality) {
                    changeHlsQuality(hls, quality);
                }
            };
            options.i18n = {
                qualityLabel: {
                    0: "Auto"
                }
            };
        }

        return new window.Plyr(mediaElement, options);
    }

    function initializeNativePlayer(mediaElement, sourceUrl) {
        mediaElement.src = sourceUrl;

        playerSessions.set(mediaElement, {
            hls: null,
            plyr: mediaElement instanceof HTMLVideoElement
                ? createVideoPlayer(mediaElement, null)
                : null
        });
    }

    function initializeHlsPlayer(mediaElement, sourceUrl) {
        const hls = new window.Hls();
        const session = {
            hls: hls,
            plyr: null
        };
        playerSessions.set(mediaElement, session);

        hls.on(window.Hls.Events.MANIFEST_PARSED, function () {
            if (mediaElement instanceof HTMLVideoElement && !session.plyr) {
                session.plyr = createVideoPlayer(mediaElement, hls);
            }
        });

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
            session.hls = null;
            showPlaybackError(mediaElement, "The stream could not be played.");
        });

        hls.loadSource(sourceUrl);
        hls.attachMedia(mediaElement);
    }

    function initializePlayer(mediaElement) {
        if (playerSessions.has(mediaElement)) {
            return;
        }

        const sourceUrl = mediaElement.dataset.hlsSource;
        if (!sourceUrl) {
            showPlaybackError(mediaElement, "No stream source is available.");
            return;
        }

        initializeWatchProgress(mediaElement);

        if (mediaElement.canPlayType("application/vnd.apple.mpegurl")) {
            initializeNativePlayer(mediaElement, sourceUrl);
            return;
        }

        if (!window.Hls || !window.Hls.isSupported()) {
            showPlaybackError(mediaElement, "HLS playback is not supported by this browser.");
            return;
        }

        initializeHlsPlayer(mediaElement, sourceUrl);
    }

    function destroyPlayer(mediaElement) {
        const progressTracker = progressTrackers.get(mediaElement);
        if (progressTracker) {
            progressTracker.save();
            progressTracker.destroy();
            progressTrackers.delete(mediaElement);
        }

        const session = playerSessions.get(mediaElement);
        if (session?.plyr) {
            session.plyr.destroy();
        }
        if (session?.hls) {
            session.hls.destroy();
        }

        mediaElement.pause();
        mediaElement.removeAttribute("src");
        mediaElement.load();
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

        modalElement.querySelectorAll("[data-hls-player]").forEach(function (mediaElement) {
            initializePlayer(mediaElement);
        });
    });

    document.addEventListener("hidden.bs.modal", function (event) {
        const modalElement = event.target;
        if (!(modalElement instanceof Element)
                || !modalElement.matches("[data-media-preview-modal]")) {
            return;
        }

        modalElement.querySelectorAll("[data-hls-player]").forEach(function (mediaElement) {
            destroyPlayer(mediaElement);
        });
    });
})();
