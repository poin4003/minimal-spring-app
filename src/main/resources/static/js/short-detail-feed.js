(function () {
    "use strict";

    const PLAYER_READY_EVENT = "app:media-player-ready";
    const PLAYER_PRELOAD_EVENT = "app:media-player-preload";
    const ACTIVE_VISIBILITY_RATIO = 0.6;
    const NORMAL_PREVIOUS_PRELOAD = 1;
    const NORMAL_NEXT_PRELOAD = 2;
    let pendingAdvanceItem = null;
    let feed = null;
    let activeItem = null;
    let itemObserver = null;
    const visibilityRatios = new Map();
    const autoPreloadedItems = new Set();

    function findSiblingItem(currentItem, direction) {
        let sibling = direction === "previous"
                ? currentItem.previousElementSibling
                : currentItem.nextElementSibling;

        while (sibling
                && !sibling.matches(".short-detail-feed-item")) {
            sibling = direction === "previous"
                    ? sibling.previousElementSibling
                    : sibling.nextElementSibling;
        }
        return sibling;
    }

    function findNextItem(currentItem) {
        return findSiblingItem(currentItem, "next");
    }

    function prepareItem(item, preload) {
        const mediaElement = item?.querySelector("[data-video-player]");
        if (!mediaElement) {
            return;
        }

        document.dispatchEvent(new CustomEvent(PLAYER_PRELOAD_EVENT, {
            detail: {
                mediaElement: mediaElement,
                preload: preload
            }
        }));
    }

    function usesConstrainedPreload() {
        const connection = navigator.connection;
        return connection?.saveData === true
                || ["slow-2g", "2g", "3g"].includes(
                        connection?.effectiveType);
    }

    function collectNearbyItems(item, previousCount, nextCount) {
        const items = new Set([item]);
        let previous = item;
        let next = item;

        for (let index = 0; index < previousCount; index += 1) {
            previous = findSiblingItem(previous, "previous");
            if (!previous) {
                break;
            }
            items.add(previous);
        }
        for (let index = 0; index < nextCount; index += 1) {
            next = findSiblingItem(next, "next");
            if (!next) {
                break;
            }
            items.add(next);
        }
        return items;
    }

    function updatePreloadWindow(item) {
        if (!item) {
            return;
        }

        const constrained = usesConstrainedPreload();
        const nextItems = collectNearbyItems(
                item,
                constrained ? 0 : NORMAL_PREVIOUS_PRELOAD,
                constrained ? 0 : NORMAL_NEXT_PRELOAD);

        autoPreloadedItems.forEach(function (previousItem) {
            if (!nextItems.has(previousItem)) {
                prepareItem(previousItem, "metadata");
                autoPreloadedItems.delete(previousItem);
            }
        });
        nextItems.forEach(function (nearbyItem) {
            prepareItem(nearbyItem, "auto");
            autoPreloadedItems.add(nearbyItem);
        });

        if (constrained) {
            prepareItem(findNextItem(item), "metadata");
        }
    }

    function requestNextPageIfNeeded(item) {
        if (!feed || !item || !window.htmx) {
            return;
        }

        const items = Array.from(feed.querySelectorAll(
                ".short-detail-feed-item"
        ));
        const activeIndex = items.indexOf(item);
        if (activeIndex < 0 || items.length - activeIndex > 3) {
            return;
        }

        const availableTriggers = Array.from(feed.querySelectorAll(
                ".short-detail-feed-item[hx-get]"
        )).reverse();
        const trigger = availableTriggers.find(function (candidate) {
            return candidate.dataset.shortLoadRequested !== "true";
        });
        if (!trigger) {
            return;
        }

        trigger.dataset.shortLoadRequested = "true";
        window.htmx.trigger(trigger, "short:load-more");
    }

    function setActiveItem(item) {
        if (!item || activeItem === item) {
            updatePreloadWindow(item);
            requestNextPageIfNeeded(item);
            return;
        }

        feed.querySelectorAll("[data-short-active='true']")
                .forEach(function (currentActive) {
                    currentActive.dataset.shortActive = "false";
                });
        item.dataset.shortActive = "true";
        activeItem = item;
        updatePreloadWindow(item);
        requestNextPageIfNeeded(item);
    }

    function positionActiveShort() {
        const activeShort = feed?.querySelector(
                "[data-short-active='true']"
        );
        if (!activeShort) {
            return;
        }

        feed.scrollTop = activeShort.offsetTop;
        activeItem = activeShort;
        updatePreloadWindow(activeShort);
        requestNextPageIfNeeded(activeShort);
    }

    function scrollToItem(item) {
        const feed = item.closest("[data-short-detail-feed]");
        if (!feed) {
            return;
        }

        const reducedMotion = window.matchMedia(
                "(prefers-reduced-motion: reduce)"
        ).matches;
        feed.scrollTo({
            top: item.offsetTop,
            behavior: reducedMotion ? "auto" : "smooth"
        });
    }

    function advanceFrom(currentItem) {
        const nextItem = findNextItem(currentItem);
        if (nextItem) {
            pendingAdvanceItem = null;
            scrollToItem(nextItem);
            return;
        }

        pendingAdvanceItem = feed?.querySelector(
                ".short-detail-feed-item.htmx-request"
        ) ? currentItem : null;
    }

    function reconcileActiveItem() {
        if (!feed) {
            return;
        }

        const feedBounds = feed.getBoundingClientRect();
        const feedCenter = feedBounds.top + feedBounds.height / 2;
        const candidate = Array.from(visibilityRatios.entries())
                .filter(function (entry) {
                    return entry[0].isConnected
                            && entry[1] >= ACTIVE_VISIBILITY_RATIO;
                })
                .sort(function (left, right) {
                    if (right[1] !== left[1]) {
                        return right[1] - left[1];
                    }
                    const leftBounds = left[0].getBoundingClientRect();
                    const rightBounds = right[0].getBoundingClientRect();
                    const leftDistance = Math.abs(
                            leftBounds.top + leftBounds.height / 2
                            - feedCenter);
                    const rightDistance = Math.abs(
                            rightBounds.top + rightBounds.height / 2
                            - feedCenter);
                    return leftDistance - rightDistance;
                })[0]?.[0];

        if (candidate) {
            setActiveItem(candidate);
        }
    }

    function observeItems(root) {
        root.querySelectorAll(".short-detail-feed-item")
                .forEach(function (item) {
                    itemObserver.observe(item);
                });
    }

    function initializeDetailFeed() {
        feed = document.querySelector("[data-short-detail-feed]");
        if (!feed) {
            return;
        }

        positionActiveShort();
        itemObserver = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                visibilityRatios.set(
                        entry.target,
                        entry.intersectionRatio);
            });
            reconcileActiveItem();
        }, {
            root: feed,
            threshold: [0, ACTIVE_VISIBILITY_RATIO, 1]
        });
        observeItems(feed);
    }

    document.addEventListener("DOMContentLoaded", initializeDetailFeed);

    document.addEventListener(PLAYER_READY_EVENT, function (event) {
        const mediaElement = event.detail.mediaElement;
        const currentItem = mediaElement.closest(
                ".short-detail-feed-item"
        );
        if (!currentItem) {
            return;
        }

        event.detail.player.on("ended", function () {
            advanceFrom(currentItem);
        });
    });

    document.addEventListener("htmx:afterSwap", function (event) {
        if (feed && (event.detail.target === feed
                || feed.contains(event.detail.target))) {
            observeItems(event.detail.target);
            updatePreloadWindow(activeItem);
            requestNextPageIfNeeded(activeItem);
        }

        if (!pendingAdvanceItem?.isConnected) {
            pendingAdvanceItem = null;
            return;
        }

        advanceFrom(pendingAdvanceItem);
    });
})();
