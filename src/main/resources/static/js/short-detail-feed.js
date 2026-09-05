(function () {
    "use strict";

    if (window.AppShortFeedInitialized === true) {
        return;
    }
    window.AppShortFeedInitialized = true;

    const PLAYER_READY_EVENT = "app:media-player-ready";
    const PLAYER_PRELOAD_EVENT = "app:media-player-preload";
    const ACTIVE_VISIBILITY_RATIO = 0.6;
    const NORMAL_PREVIOUS_PRELOAD = 1;
    const NORMAL_NEXT_PRELOAD = 2;

    document.addEventListener("alpine:init", function () {
        Alpine.data("shortFeed", () => ({
            activeItem: null,
            itemObserver: null,
            mutationObserver: null,
            visibilityRatios: new Map(),
            autoPreloadedItems: new Set(),
            pendingAdvanceItem: null,
            playerReadyHandler: null,

            init() {
                this.positionActiveShort();
                this.itemObserver = new IntersectionObserver(entries => {
                    entries.forEach(entry => this.visibilityRatios.set(
                        entry.target,
                        entry.intersectionRatio));
                    this.reconcileActiveItem();
                }, {
                    root: this.$root,
                    threshold: [0, ACTIVE_VISIBILITY_RATIO, 1]
                });
                this.observeItems(this.$root);

                this.playerReadyHandler = event => {
                    const currentItem = event.detail?.mediaElement?.closest(
                        ".short-detail-feed-item");
                    if (currentItem == null
                            || !this.$root.contains(currentItem)) {
                        return;
                    }
                    event.detail.player.on("ended", () => {
                        this.advanceFrom(currentItem);
                    });
                };
                document.addEventListener(
                    PLAYER_READY_EVENT,
                    this.playerReadyHandler);

                this.mutationObserver = new MutationObserver(records => {
                    records.forEach(record => record.addedNodes.forEach(node => {
                        if (node instanceof Element) {
                            this.observeItems(node);
                        }
                    }));
                    this.updatePreloadWindow(this.activeItem);
                    this.requestNextPageIfNeeded(this.activeItem);
                    if (this.pendingAdvanceItem?.isConnected) {
                        this.advanceFrom(this.pendingAdvanceItem);
                    } else {
                        this.pendingAdvanceItem = null;
                    }
                });
                this.mutationObserver.observe(this.$root, {
                    childList: true
                });
            },

            findSiblingItem(currentItem, direction) {
                let sibling = direction === "previous"
                    ? currentItem?.previousElementSibling
                    : currentItem?.nextElementSibling;
                while (sibling != null
                        && !sibling.matches(".short-detail-feed-item")) {
                    sibling = direction === "previous"
                        ? sibling.previousElementSibling
                        : sibling.nextElementSibling;
                }
                return sibling;
            },

            prepareItem(item, preload) {
                const mediaElement = item?.querySelector("[data-video-player]");
                if (mediaElement == null) {
                    return;
                }
                document.dispatchEvent(new CustomEvent(PLAYER_PRELOAD_EVENT, {
                    detail: { mediaElement, preload }
                }));
            },

            collectNearbyItems(item, previousCount, nextCount) {
                const items = new Set([item]);
                let previous = item;
                let next = item;
                for (let index = 0; index < previousCount; index += 1) {
                    previous = this.findSiblingItem(previous, "previous");
                    if (previous == null) {
                        break;
                    }
                    items.add(previous);
                }
                for (let index = 0; index < nextCount; index += 1) {
                    next = this.findSiblingItem(next, "next");
                    if (next == null) {
                        break;
                    }
                    items.add(next);
                }
                return items;
            },

            updatePreloadWindow(item) {
                if (item == null) {
                    return;
                }
                const connection = navigator.connection;
                const constrained = connection?.saveData === true
                    || ["slow-2g", "2g", "3g"].includes(
                        connection?.effectiveType);
                const nextItems = this.collectNearbyItems(
                    item,
                    constrained ? 0 : NORMAL_PREVIOUS_PRELOAD,
                    constrained ? 0 : NORMAL_NEXT_PRELOAD);

                this.autoPreloadedItems.forEach(previousItem => {
                    if (!nextItems.has(previousItem)) {
                        this.prepareItem(previousItem, "metadata");
                        this.autoPreloadedItems.delete(previousItem);
                    }
                });
                nextItems.forEach(nearbyItem => {
                    this.prepareItem(nearbyItem, "auto");
                    this.autoPreloadedItems.add(nearbyItem);
                });
                if (constrained) {
                    this.prepareItem(
                        this.findSiblingItem(item, "next"),
                        "metadata");
                }
            },

            requestNextPageIfNeeded(item) {
                if (item == null || window.htmx == null) {
                    return;
                }
                const items = Array.from(this.$root.querySelectorAll(
                    ".short-detail-feed-item"));
                const activeIndex = items.indexOf(item);
                if (activeIndex < 0 || items.length - activeIndex > 3) {
                    return;
                }
                const trigger = Array.from(this.$root.querySelectorAll(
                    ".short-detail-feed-item[hx-get]"
                )).reverse().find(candidate =>
                    candidate.dataset.shortLoadRequested !== "true");
                if (trigger == null) {
                    return;
                }
                trigger.dataset.shortLoadRequested = "true";
                window.htmx.trigger(trigger, "short:load-more");
            },

            setActiveItem(item) {
                if (item == null) {
                    return;
                }
                if (this.activeItem !== item) {
                    this.$root.querySelectorAll("[data-short-active='true']")
                        .forEach(current => {
                            current.dataset.shortActive = "false";
                        });
                    item.dataset.shortActive = "true";
                    this.activeItem = item;
                }
                this.updatePreloadWindow(item);
                this.requestNextPageIfNeeded(item);
            },

            positionActiveShort() {
                const item = this.$root.querySelector(
                    "[data-short-active='true']");
                if (item == null) {
                    return;
                }
                this.$root.scrollTop = item.offsetTop;
                this.activeItem = item;
                this.updatePreloadWindow(item);
                this.requestNextPageIfNeeded(item);
            },

            reconcileActiveItem() {
                const feedBounds = this.$root.getBoundingClientRect();
                const feedCenter = feedBounds.top + feedBounds.height / 2;
                const candidate = Array.from(this.visibilityRatios.entries())
                    .filter(([item, ratio]) => item.isConnected
                        && ratio >= ACTIVE_VISIBILITY_RATIO)
                    .sort((left, right) => {
                        if (right[1] !== left[1]) {
                            return right[1] - left[1];
                        }
                        const leftBounds = left[0].getBoundingClientRect();
                        const rightBounds = right[0].getBoundingClientRect();
                        return Math.abs(
                            leftBounds.top + leftBounds.height / 2
                            - feedCenter)
                            - Math.abs(
                                rightBounds.top + rightBounds.height / 2
                                - feedCenter);
                    })[0]?.[0];
                if (candidate != null) {
                    this.setActiveItem(candidate);
                }
            },

            observeItems(root) {
                if (this.itemObserver == null) {
                    return;
                }
                if (root.matches?.(".short-detail-feed-item")) {
                    this.itemObserver.observe(root);
                }
                root.querySelectorAll?.(".short-detail-feed-item")
                    .forEach(item => this.itemObserver.observe(item));
            },

            advanceFrom(currentItem) {
                const nextItem = this.findSiblingItem(currentItem, "next");
                if (nextItem == null) {
                    this.pendingAdvanceItem = this.$root.querySelector(
                        ".short-detail-feed-item.htmx-request"
                    ) == null ? null : currentItem;
                    return;
                }

                this.pendingAdvanceItem = null;
                const reducedMotion = window.matchMedia(
                    "(prefers-reduced-motion: reduce)").matches;
                this.$root.scrollTo({
                    top: nextItem.offsetTop,
                    behavior: reducedMotion ? "auto" : "smooth"
                });
            },

            destroy() {
                this.itemObserver?.disconnect();
                this.mutationObserver?.disconnect();
                document.removeEventListener(
                    PLAYER_READY_EVENT,
                    this.playerReadyHandler);
                this.visibilityRatios.clear();
                this.autoPreloadedItems.clear();
            }
        }));
    });
})();
