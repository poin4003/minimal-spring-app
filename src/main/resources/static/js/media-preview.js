(function () {
    "use strict";

    const WATCH_PROGRESS_PREFIX = "media-watch-progress:";
    const SAVE_INTERVAL_SECONDS = 5;
    const INITIAL_HLS_BANDWIDTH = 10_000_000;
    const PLAYER_READY_EVENT = "app:media-player-ready";
    const PLAYER_DESTROYING_EVENT = "app:media-player-destroying";
    const PLAYER_PRELOAD_EVENT = "app:media-player-preload";
    const playerSessions = new Map();
    const progressTrackers = new Map();
    let activePlaybackPlayer = null;

    const lazyPlayerObserver = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (!entry.isIntersecting) {
                return;
            }

            lazyPlayerObserver.unobserve(entry.target);
            initializePlayer(entry.target);
        });
    }, {
        rootMargin: "75% 0px",
        threshold: 0.01
    });

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
            preload: mediaElement.preload === "auto" ? "auto" : "metadata",
            muted: mediaElement.hasAttribute("data-feed-autoplay"),
            responsive: true,
            fluid: isVideo,
            audioOnlyMode: !isVideo,
            playbackRates: [0.5, 0.75, 1, 1.25, 1.5, 1.75, 2],
            html5: {
                vhs: {
                    overrideNative: true,
                    bandwidth: INITIAL_HLS_BANDWIDTH,
                    enableLowInitialPlaylist: false,
                    useBandwidthFromLocalStorage: false,
                    limitRenditionByPlayerDimensions: false,
                    useNetworkInformationApi: false
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

    function registerExclusivePlayback(player) {
        player.on("play", function () {
            const previousPlayer = activePlaybackPlayer;
            activePlaybackPlayer = player;

            if (previousPlayer
                    && previousPlayer !== player
                    && !previousPlayer.isDisposed()) {
                previousPlayer.pause();
            }
        });

        player.on("dispose", function () {
            if (activePlaybackPlayer === player) {
                activePlaybackPlayer = null;
            }
        });
    }

    function initializeCurrentQualityDisplay(player, qualitySelector) {
        const qualityLevels = player.qualityLevels();

        function resolveCurrentResolution() {
            const selectedIndex = qualityLevels.selectedIndex;
            if (selectedIndex < 0 || selectedIndex >= qualityLevels.length) {
                return null;
            }

            const level = qualityLevels[selectedIndex];
            const resolution = Math.min(level.width || 0, level.height || 0);
            return resolution > 0 ? resolution + "p" : null;
        }

        function updateQualityLabel() {
            const selectedQuality = qualitySelector.getCurrentQuality();
            const currentResolution = resolveCurrentResolution();
            if (selectedQuality !== "auto") {
                qualitySelector.setButtonInnerText(selectedQuality + "p");
                return;
            }

            const autoLabel = player.localize("Auto");
            qualitySelector.setButtonInnerText(currentResolution
                ? autoLabel + " / " + currentResolution
                : autoLabel);
        }

        qualityLevels.on("change", updateQualityLabel);
        qualityLevels.on("addqualitylevel", updateQualityLabel);
        player.on("loadedmetadata", updateQualityLabel);
        player.on("dispose", function () {
            qualityLevels.off("change", updateQualityLabel);
            qualityLevels.off("addqualitylevel", updateQualityLabel);
        });
    }

    function initializePlayer(mediaElement) {
        if (!(mediaElement instanceof HTMLMediaElement)
                || playerSessions.has(mediaElement)) {
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

        const preload = mediaElement.preload === "auto"
                ? "auto"
                : "metadata";
        const player = window.videojs(
                mediaElement,
                createPlayerOptions(mediaElement));
        registerExclusivePlayback(player);
        playerSessions.set(mediaElement, {
            player: player,
            preload: preload
        });

        player.ready(function () {
            if (mediaElement instanceof HTMLVideoElement
                    && typeof player.hlsQualitySelector === "function") {
                const qualitySelector = player.hlsQualitySelector({
                    displayCurrentQuality: true
                });
                initializeCurrentQualityDisplay(player, qualitySelector);
            }

            initializeWatchProgress(mediaElement, player);
            player.src({
                src: sourceUrl,
                type: "application/x-mpegURL"
            });
            if (preload === "auto") {
                player.load();
            }

            document.dispatchEvent(new CustomEvent(PLAYER_READY_EVENT, {
                detail: {
                    mediaElement: mediaElement,
                    player: player
                }
            }));
        });

        player.on("error", function () {
            showPlaybackError(mediaElement, "messageStreamFailed");
        });
    }

    function destroyPlayer(mediaElement) {
        lazyPlayerObserver.unobserve(mediaElement);
        const progressTracker = progressTrackers.get(mediaElement);
        if (progressTracker) {
            progressTracker.save();
            progressTracker.destroy();
            progressTrackers.delete(mediaElement);
        }

        const session = playerSessions.get(mediaElement);
        if (session?.player && !session.player.isDisposed()) {
            document.dispatchEvent(new CustomEvent(PLAYER_DESTROYING_EVENT, {
                detail: {
                    mediaElement: mediaElement,
                    player: session.player
                }
            }));
            session.player.dispose();
        }
        playerSessions.delete(mediaElement);
    }

    function preparePlayer(mediaElement, requestedPreload) {
        if (!(mediaElement instanceof HTMLMediaElement)) {
            return;
        }

        const preload = requestedPreload === "auto"
                ? "auto"
                : "metadata";
        mediaElement.preload = preload;
        lazyPlayerObserver.unobserve(mediaElement);

        const session = playerSessions.get(mediaElement);
        if (!session) {
            initializePlayer(mediaElement);
            return;
        }
        if (session.player.isDisposed() || session.preload === preload) {
            return;
        }

        session.preload = preload;
        session.player.preload(preload);
        if (preload === "auto"
                && session.player.paused()
                && session.player.readyState() < 3) {
            session.player.load();
        }
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

    function initializeLazyPlayers(root) {
        if (!(root instanceof Document || root instanceof Element)) {
            return;
        }

        if (root instanceof Element
                && root.matches(
                    "[data-video-player][data-lazy-initialize]"
                )) {
            lazyPlayerObserver.observe(root);
        }
        root.querySelectorAll(
            "[data-video-player][data-lazy-initialize]"
        ).forEach(function (mediaElement) {
            lazyPlayerObserver.observe(mediaElement);
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
        initializeLazyPlayers(document);
    });

    document.addEventListener("htmx:afterSwap", function (event) {
        initializeAutoPlayers(event.detail.target);
        initializeLazyPlayers(event.detail.target);
    });

    document.addEventListener("htmx:beforeCleanupElement", function (event) {
        destroyPlayers(event.detail.elt);
    });

    document.addEventListener(PLAYER_PRELOAD_EVENT, function (event) {
        preparePlayer(
                event.detail?.mediaElement,
                event.detail?.preload);
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
