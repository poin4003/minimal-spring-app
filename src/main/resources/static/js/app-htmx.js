(function () {
    "use strict";

    if (window.AppHtmxInitialized === true) {
        return;
    }
    window.AppHtmxInitialized = true;

    const CSRF_COOKIE_NAME = "XSRF-TOKEN";
    const CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    const BODY_CLASS_STORAGE_PREFIX = "app-body-class:";
    const desktopViewport = window.matchMedia("(min-width: 992px)");
    const busyTargetCounts = new WeakMap();

    function setRequestTargetBusy(target, busy) {
        if (!(target instanceof Element)) {
            return;
        }

        const currentCount = busyTargetCounts.get(target) ?? 0;
        const nextCount = busy
            ? currentCount + 1
            : Math.max(0, currentCount - 1);

        if (nextCount === 0) {
            busyTargetCounts.delete(target);
            target.classList.remove("app-request-busy");
            target.removeAttribute("aria-busy");
            return;
        }

        busyTargetCounts.set(target, nextCount);
        target.classList.add("app-request-busy");
        target.setAttribute("aria-busy", "true");
    }

    function bodyClassStorageKey(path) {
        return BODY_CLASS_STORAGE_PREFIX + path;
    }

    function storeBodyClass(path, className) {
        sessionStorage.setItem(bodyClassStorageKey(path), className);
        sessionStorage.setItem(
            bodyClassStorageKey(path.split("?")[0]),
            className);
    }

    function isPageSwap(event) {
        const targetId = event.detail.target?.id;
        return event.detail.boosted
            || targetId === "app-page-content"
            || targetId === "app-social-page-content";
    }

    function syncBodyClassFromResponse(event) {
        if (!isPageSwap(event)
                || event.detail.xhr?.responseText == null) {
            return;
        }

        const responseDocument = new DOMParser().parseFromString(
            event.detail.xhr.responseText,
            "text/html");
        const responseBody = responseDocument.body;
        if (responseBody == null) {
            return;
        }

        document.body.className = responseBody.className;
        if (responseDocument.title) {
            document.title = responseDocument.title;
        }
        const responseUrl = new URL(
            event.detail.xhr.responseURL || window.location.href,
            window.location.href);
        storeBodyClass(
            responseUrl.pathname + responseUrl.search,
            responseBody.className);
    }

    function restoreBodyClass() {
        const currentPath = window.location.pathname + window.location.search;
        const className = sessionStorage.getItem(
            bodyClassStorageKey(currentPath))
            ?? sessionStorage.getItem(bodyClassStorageKey(
                window.location.pathname));
        if (className != null) {
            document.body.className = className;
        }
    }

    function closeMobileSidebar() {
        if (desktopViewport.matches || typeof bootstrap === "undefined") {
            return;
        }

        const sidebar = document.getElementById("app-sidebar");
        const offcanvas = sidebar == null
            ? null
            : bootstrap.Offcanvas.getInstance(sidebar);
        offcanvas?.hide();
    }

    function dispatchUiEvent(name, detail = {}) {
        window.dispatchEvent(new CustomEvent(name, { detail }));
    }

    document.addEventListener("htmx:configRequest", function (event) {
        const csrfToken = window.AppUi.readCookie(CSRF_COOKIE_NAME);
        if (csrfToken) {
            event.detail.headers[CSRF_HEADER_NAME] = csrfToken;
        }
    });

    document.addEventListener("htmx:beforeRequest", function (event) {
        dispatchUiEvent("app-request-start");
        if (event.detail.elt.closest("[data-app-loader='manual']") != null) {
            return;
        }

        setRequestTargetBusy(event.detail.target, true);
        if (event.detail.elt.closest("[data-app-loader='global']") != null) {
            window.AppUi.showLoader();
        }
    });

    document.addEventListener("htmx:beforeOnLoad", function (event) {
        const redirectPath = event.detail.xhr?.getResponseHeader("HX-Redirect");
        if (!redirectPath) {
            return;
        }

        event.preventDefault();
        window.location.replace(redirectPath);
    });

    document.addEventListener("htmx:beforeSwap", syncBodyClassFromResponse);

    document.addEventListener("htmx:afterRequest", function (event) {
        setRequestTargetBusy(event.detail.target, false);
        window.AppUi.hideLoader();

        const element = event.detail.elt;
        if (element instanceof HTMLFormElement
                && element.matches("[data-app-modal-form]")) {
            dispatchUiEvent("app-modal-request-complete", {
                element,
                successful: event.detail.successful
            });
        }
    });

    ["htmx:sendError", "htmx:timeout"].forEach(eventName => {
        document.addEventListener(eventName, function (event) {
            setRequestTargetBusy(event.detail.target, false);
            window.AppUi.hideLoader();
            dispatchUiEvent("app-request-error", { type: "connection" });
            dispatchUiEvent("app-modal-request-failed", {
                element: event.detail.elt
            });
        });
    });

    document.addEventListener("htmx:responseError", function (event) {
        setRequestTargetBusy(event.detail.target, false);
        window.AppUi.hideLoader();
        dispatchUiEvent("app-request-error", { type: "request" });
        dispatchUiEvent("app-modal-request-failed", {
            element: event.detail.elt
        });
    });

    document.addEventListener("htmx:historyRestore", function () {
        restoreBodyClass();
        dispatchUiEvent("app-navigation-changed", {
            path: window.location.pathname
        });
        window.AppUi.refreshSidebar();
    });

    document.addEventListener("htmx:afterSwap", function (event) {
        if (event.detail.target instanceof Element
                && event.detail.target.id === "app-modal-host") {
            dispatchUiEvent("app-modal-loaded", {
                target: event.detail.target
            });
        }

        if (!isPageSwap(event)) {
            return;
        }

        dispatchUiEvent("app-navigation-changed", {
            path: window.location.pathname
        });
        window.AppUi.refreshSidebar();
        closeMobileSidebar();
    });

    document.addEventListener("DOMContentLoaded", function () {
        storeBodyClass(
            window.location.pathname + window.location.search,
            document.body.className);
        dispatchUiEvent("app-navigation-changed", {
            path: window.location.pathname
        });
    });
})();
