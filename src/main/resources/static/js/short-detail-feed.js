(function () {
    "use strict";

    const PLAYER_READY_EVENT = "app:media-player-ready";
    let pendingAdvanceItem = null;

    function positionActiveShort() {
        const feed = document.querySelector("[data-short-detail-feed]");
        const activeShort = feed?.querySelector(
                "[data-short-active='true']"
        );
        if (!activeShort) {
            return;
        }

        feed.scrollTop = activeShort.offsetTop;
    }

    function findNextItem(currentItem) {
        let nextItem = currentItem.nextElementSibling;

        while (nextItem
                && !nextItem.matches(".short-detail-feed-item")) {
            nextItem = nextItem.nextElementSibling;
        }

        return nextItem;
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

        pendingAdvanceItem = currentItem.hasAttribute("hx-get")
                ? currentItem
                : null;
    }

    document.addEventListener("DOMContentLoaded", positionActiveShort);

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

    document.addEventListener("htmx:afterSwap", function () {
        if (!pendingAdvanceItem?.isConnected) {
            pendingAdvanceItem = null;
            return;
        }

        advanceFrom(pendingAdvanceItem);
    });
})();
