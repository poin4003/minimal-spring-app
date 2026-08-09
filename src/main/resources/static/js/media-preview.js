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
        const modalElement = mediaElement.closest(
                "[data-media-preview-modal], [data-media-preview]");
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

    function initializeAutoPlayers(root) {
        if (!(root instanceof Document || root instanceof Element)) {
            return;
        }

        root.querySelectorAll("[data-video-player][data-auto-initialize]")
            .forEach(function (mediaElement) {
                initializePlayer(mediaElement);
            });
    }

    function initializePlayers(root) {
        if (!(root instanceof Element)) {
            return;
        }

        if (root.matches("[data-video-player]")) {
            initializePlayer(root);
        }
        root.querySelectorAll("[data-video-player]")
            .forEach(function (mediaElement) {
                initializePlayer(mediaElement);
            });
    }

    function scrollActiveThumbnail(root, behavior) {
        if (!(root instanceof Element)) {
            return;
        }

        const activeThumbnail = root.querySelector(
                ".post-media-gallery-thumbnails .active"
        );
        activeThumbnail?.scrollIntoView({
            behavior: behavior,
            block: "nearest",
            inline: "center"
        });
    }

    function pausePlayer(mediaElement) {
        const progressTracker = progressTrackers.get(mediaElement);
        progressTracker?.save();

        const session = playerSessions.get(mediaElement);
        if (session?.player && !session.player.isDisposed()) {
            session.player.pause();
        }
    }

    function pausePlayers(root) {
        if (!(root instanceof Element)) {
            return;
        }

        if (root.matches("[data-video-player]")) {
            pausePlayer(root);
        }
        root.querySelectorAll("[data-video-player]")
            .forEach(function (mediaElement) {
                pausePlayer(mediaElement);
            });
    }

    function destroyPlayers(root) {
        if (!(root instanceof Element)) {
            return;
        }

        if (root.matches("[data-video-player]")) {
            destroyPlayer(root);
        }
        root.querySelectorAll("[data-video-player]")
            .forEach(function (mediaElement) {
                destroyPlayer(mediaElement);
            });
    }

    window.addEventListener("pagehide", function () {
        progressTrackers.forEach(function (progressTracker) {
            progressTracker.save();
        });
    });

    document.addEventListener("DOMContentLoaded", function () {
        initializeAutoPlayers(document);
    });

    document.addEventListener("htmx:afterSwap", function (event) {
        initializeAutoPlayers(event.detail.target);
    });

    document.addEventListener("htmx:beforeCleanupElement", function (event) {
        destroyPlayers(event.detail.elt);
    });

    document.addEventListener("shown.bs.modal", function (event) {
        const modalElement = event.target;
        if (!(modalElement instanceof Element)
                || !modalElement.matches("[data-media-preview-modal]")) {
            return;
        }

        if (modalElement.matches("[data-post-media-gallery-modal]")) {
            initializePlayers(
                modalElement.querySelector(".carousel-item.active")
            );
            scrollActiveThumbnail(modalElement, "auto");
            return;
        }

        initializePlayers(modalElement);
    });

    document.addEventListener("slide.bs.carousel", function (event) {
        const carouselElement = event.target;
        if (!(carouselElement instanceof Element)
                || carouselElement.closest(
                    "[data-post-media-gallery-modal]"
                ) == null) {
            return;
        }

        pausePlayers(
            carouselElement.querySelector(".carousel-item.active")
        );
    });

    document.addEventListener("slid.bs.carousel", function (event) {
        const carouselElement = event.target;
        if (!(carouselElement instanceof Element)
                || carouselElement.closest(
                    "[data-post-media-gallery-modal]"
                ) == null
                || !(event.relatedTarget instanceof Element)) {
            return;
        }

        initializePlayers(event.relatedTarget);
        scrollActiveThumbnail(carouselElement, "smooth");
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
