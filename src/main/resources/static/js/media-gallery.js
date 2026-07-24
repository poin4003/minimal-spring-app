(function () {
    const hoverMediaQuery = window.matchMedia(
        "(hover: hover) and (pointer: fine)");
    let activePreview = null;

    function stopPreview(card) {
        if (!activePreview
                || (card && activePreview.card !== card)) {
            return;
        }

        const preview = activePreview;
        activePreview = null;
        preview.mount.classList.add("d-none");
        preview.player.dispose();
        preview.mount.replaceChildren();
    }

    function startPreview(card) {
        if (!hoverMediaQuery.matches
                || typeof window.videojs !== "function"
                || activePreview?.card === card) {
            return;
        }

        const mount = card.querySelector("[data-media-hover-preview]");
        if (!mount) {
            return;
        }

        stopPreview();

        const video = document.createElement("video");
        video.className = "video-js";
        video.muted = true;
        video.loop = true;
        video.playsInline = true;
        mount.replaceChildren(video);

        const player = window.videojs(video, {
            autoplay: true,
            controls: false,
            loop: true,
            muted: true,
            preload: "auto",
            sources: [{
                src: mount.dataset.mediaHoverPreview,
                type: "application/x-mpegURL"
            }],
            html5: {
                vhs: {
                    enableLowInitialPlaylist: true
                }
            }
        });

        activePreview = {
            card,
            mount,
            player
        };

        player.one("playing", function () {
            if (activePreview?.player === player) {
                mount.classList.remove("d-none");
            }
        });

        player.ready(function () {
            if (activePreview?.player !== player) {
                return;
            }

            const playRequest = player.play();
            if (playRequest && typeof playRequest.catch === "function") {
                playRequest.catch(function () {
                    stopPreview(card);
                });
            }
        });
    }

    document.addEventListener("pointerover", function (event) {
        if (!(event.target instanceof Element)) {
            return;
        }

        const card = event.target.closest("[data-media-gallery-card]");
        if (!card
                || (event.relatedTarget instanceof Node
                    && card.contains(event.relatedTarget))) {
            return;
        }

        startPreview(card);
    });

    document.addEventListener("pointerout", function (event) {
        if (!(event.target instanceof Element)) {
            return;
        }

        const card = event.target.closest("[data-media-gallery-card]");
        if (!card
                || (event.relatedTarget instanceof Node
                    && card.contains(event.relatedTarget))) {
            return;
        }

        stopPreview(card);
    });

    hoverMediaQuery.addEventListener("change", function (event) {
        if (!event.matches) {
            stopPreview();
        }
    });

    document.addEventListener("error", function (event) {
        if (!(event.target instanceof HTMLImageElement)
                || !event.target.matches("[data-media-thumbnail]")) {
            return;
        }

        event.target.classList.add("d-none");
    }, true);
})();
