(function () {
    "use strict";

    const PLAYER_READY_EVENT = "app:media-player-ready";
    const PLAYER_DESTROYING_EVENT = "app:media-player-destroying";
    const START_VISIBILITY_RATIO = 0.65;
    const STOP_VISIBILITY_RATIO = 0.35;
    const SWITCH_SCORE_ADVANTAGE = 0.15;
    const entries = new Map();
    const observedEntries = new Map();
    let activeEntry = null;
    let reconcileFrame = null;
    let playVersion = 0;

    const observer = new IntersectionObserver(function (observations) {
        observations.forEach(function (observation) {
            const entry = observedEntries.get(observation.target);
            if (!entry) {
                return;
            }

            entry.visibilityRatio = observation.intersectionRatio;
            if (entry.visibilityRatio < STOP_VISIBILITY_RATIO) {
                entry.manualPause = false;
            }
        });
        scheduleReconcile();
    }, {
        threshold: [0, STOP_VISIBILITY_RATIO, START_VISIBILITY_RATIO, 1]
    });

    function canAutoplay() {
        const reducedMotion = window.matchMedia(
                "(prefers-reduced-motion: reduce)"
        ).matches;
        const saveData = navigator.connection?.saveData === true;
        return !reducedMotion && !saveData;
    }

    function hasOpenModal() {
        return document.querySelector(".modal.show") != null;
    }

    function score(entry) {
        const bounds = entry.wrapper.getBoundingClientRect();
        const elementCenter = bounds.top + bounds.height / 2;
        const viewportCenter = window.innerHeight / 2;
        const centerPenalty = Math.min(
                Math.abs(elementCenter - viewportCenter)
                        / Math.max(window.innerHeight, 1),
                1
        );
        return entry.visibilityRatio - centerPenalty * 0.1;
    }

    function findBestCandidate() {
        return Array.from(entries.values())
                .filter(function (entry) {
                    return entry.wrapper.isConnected
                            && entry.autoplayEligible
                            && entry.visibilityRatio >= START_VISIBILITY_RATIO
                            && !entry.manualPause;
                })
                .sort(function (left, right) {
                    return score(right) - score(left);
                })[0] || null;
    }

    function pauseEntry(entry) {
        if (!entry || entry.player.isDisposed()) {
            return;
        }

        entry.programmaticPause = true;
        entry.player.pause();
        entry.programmaticPause = false;
    }

    function clearActiveEntry() {
        playVersion += 1;
        pauseEntry(activeEntry);
        activeEntry = null;
    }

    function playEntry(entry) {
        if (!entry || entry.player.isDisposed()) {
            return;
        }

        const currentPlayVersion = ++playVersion;
        activeEntry = entry;
        entry.player.muted(true);
        requestAutomaticPlay(entry, currentPlayVersion);
    }

    function requestAutomaticPlay(entry, currentPlayVersion) {
        entry.automaticPlayRequest = true;
        const playRequest = entry.player.play();
        if (playRequest && typeof playRequest.catch === "function") {
            playRequest.catch(function () {
                entry.automaticPlayRequest = false;
                if (activeEntry !== entry
                        || playVersion !== currentPlayVersion
                        || entry.player.isDisposed()) {
                    return;
                }

                entry.manualPause = true;
                activeEntry = null;
            });
        }
    }

    function reconcile() {
        reconcileFrame = null;

        if (document.hidden || hasOpenModal()) {
            clearActiveEntry();
            return;
        }

        if (activeEntry) {
            const activeStillVisible = activeEntry.wrapper.isConnected
                    && activeEntry.visibilityRatio >= STOP_VISIBILITY_RATIO;
            if (!activeStillVisible) {
                clearActiveEntry();
            } else if (!activeEntry.automaticPlayback) {
                return;
            } else if (!canAutoplay()) {
                clearActiveEntry();
                return;
            }
        }

        const candidate = canAutoplay() ? findBestCandidate() : null;
        if (activeEntry) {
            const candidateClearlyBetter = candidate
                    && candidate !== activeEntry
                    && score(candidate)
                            >= score(activeEntry) + SWITCH_SCORE_ADVANTAGE;

            if (!candidateClearlyBetter) {
                return;
            }
            clearActiveEntry();
        }

        if (candidate) {
            playEntry(candidate);
        }
    }

    function scheduleReconcile() {
        if (reconcileFrame != null) {
            return;
        }
        reconcileFrame = window.requestAnimationFrame(reconcile);
    }

    function registerPlayer(mediaElement, player) {
        if (entries.has(mediaElement)) {
            return;
        }

        const wrapper = mediaElement.closest("[data-feed-playback]");
        if (!wrapper) {
            return;
        }

        const entry = {
            mediaElement: mediaElement,
            player: player,
            wrapper: wrapper,
            visibilityRatio: 0,
            autoplayEligible: mediaElement.hasAttribute("data-feed-autoplay"),
            automaticPlayRequest: false,
            automaticPlayback: false,
            manualPause: false,
            programmaticPause: false
        };

        player.on("play", function () {
            entry.automaticPlayback = entry.automaticPlayRequest;
            entry.automaticPlayRequest = false;
            entry.manualPause = false;
            activeEntry = entry;
        });
        player.on("pause", function () {
            if (!entry.programmaticPause && activeEntry === entry) {
                entry.manualPause = entry.autoplayEligible;
                entry.automaticPlayback = false;
                activeEntry = null;
                scheduleReconcile();
            }
        });
        player.on("ended", function () {
            entry.manualPause = entry.autoplayEligible;
            entry.automaticPlayback = false;
            if (activeEntry === entry) {
                activeEntry = null;
            }
        });

        entries.set(mediaElement, entry);
        observedEntries.set(wrapper, entry);
        observer.observe(wrapper);
    }

    function unregisterPlayer(mediaElement) {
        const entry = entries.get(mediaElement);
        if (!entry) {
            return;
        }

        observer.unobserve(entry.wrapper);
        observedEntries.delete(entry.wrapper);
        if (activeEntry === entry) {
            activeEntry = null;
            playVersion += 1;
        }
        entries.delete(mediaElement);
        scheduleReconcile();
    }

    document.addEventListener(PLAYER_READY_EVENT, function (event) {
        registerPlayer(event.detail.mediaElement, event.detail.player);
    });

    document.addEventListener(PLAYER_DESTROYING_EVENT, function (event) {
        unregisterPlayer(event.detail.mediaElement);
    });

    document.addEventListener("visibilitychange", scheduleReconcile);
    document.addEventListener("show.bs.modal", clearActiveEntry);
    document.addEventListener("hidden.bs.modal", scheduleReconcile);
    window.addEventListener("scroll", scheduleReconcile, { passive: true });
    window.addEventListener("resize", scheduleReconcile);
    window.addEventListener("pagehide", clearActiveEntry);
})();
