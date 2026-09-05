(function () {
    "use strict";

    if (window.AppMediaGalleryInitialized === true) {
        return;
    }
    window.AppMediaGalleryInitialized = true;

    const hoverMediaQuery = window.matchMedia(
        "(hover: hover) and (pointer: fine)");
    let activePreview = null;

    function stopActivePreview(component = null) {
        if (activePreview == null
                || component != null && activePreview !== component) {
            return;
        }

        const preview = activePreview;
        activePreview = null;
        preview.stop();
    }

    document.addEventListener("alpine:init", function () {
        Alpine.data("mediaHoverPreview", () => ({
            player: null,
            mount: null,
            entering: null,
            leaving: null,

            init() {
                this.mount = this.$root.querySelector(
                    "[data-media-hover-preview]");
                this.entering = () => this.start();
                this.leaving = () => stopActivePreview(this);
                this.$root.addEventListener("pointerenter", this.entering);
                this.$root.addEventListener("pointerleave", this.leaving);
            },

            start() {
                if (!hoverMediaQuery.matches
                        || this.mount == null
                        || typeof window.videojs !== "function"
                        || activePreview === this) {
                    return;
                }

                stopActivePreview();

                const video = document.createElement("video");
                video.className = "video-js";
                video.muted = true;
                video.loop = true;
                video.playsInline = true;
                this.mount.replaceChildren(video);

                this.player = window.videojs(video, {
                    autoplay: true,
                    controls: false,
                    loop: true,
                    muted: true,
                    preload: "auto",
                    sources: [{
                        src: this.mount.dataset.mediaHoverPreview,
                        type: "application/x-mpegURL"
                    }],
                    html5: {
                        vhs: { enableLowInitialPlaylist: true }
                    }
                });
                activePreview = this;

                this.player.one("playing", () => {
                    if (activePreview !== this) {
                        return;
                    }
                    this.$root.classList.add("is-media-previewing");
                    this.mount.classList.remove("d-none");
                });

                this.player.ready(() => {
                    if (activePreview !== this) {
                        return;
                    }
                    const playRequest = this.player.play();
                    playRequest?.catch?.(() => stopActivePreview(this));
                });
            },

            stop() {
                this.$root.classList.remove("is-media-previewing");
                this.mount?.classList.add("d-none");
                if (this.player != null && !this.player.isDisposed()) {
                    this.player.dispose();
                }
                this.player = null;
                this.mount?.replaceChildren();
            },

            destroy() {
                if (activePreview === this) {
                    activePreview = null;
                }
                this.stop();
                this.$root.removeEventListener(
                    "pointerenter",
                    this.entering);
                this.$root.removeEventListener(
                    "pointerleave",
                    this.leaving);
            }
        }));
    });

    hoverMediaQuery.addEventListener("change", function (event) {
        if (!event.matches) {
            stopActivePreview();
        }
    });

    document.addEventListener("error", function (event) {
        if (event.target instanceof HTMLImageElement
                && event.target.matches("[data-media-thumbnail]")) {
            event.target.classList.add("d-none");
        }
    }, true);
})();
