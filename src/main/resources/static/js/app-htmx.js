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
    const requestContexts = new WeakMap();
    const activeRequests = new Set();
    let pageEpoch = 0;
    let latestPageRequest = 0;

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

    function isPageTarget(target) {
        return target instanceof Element
            && (target.id === "app-page-content"
                || target.id === "app-social-page-content");
    }

    function isPageSwap(event) {
        return isPageTarget(event.detail.target);
    }

    function abortActiveRequests() {
        Array.from(activeRequests).forEach(xhr => {
            if (xhr.readyState !== XMLHttpRequest.DONE) {
                xhr.abort();
            }
        });
        activeRequests.clear();
    }

    function registerRequest(event) {
        const xhr = event.detail.xhr;
        if (!(xhr instanceof XMLHttpRequest)) {
            return;
        }

        if (isPageTarget(event.detail.target)) {
            abortActiveRequests();
            const sequence = ++latestPageRequest;
            requestContexts.set(xhr, {
                page: true,
                sequence: sequence
            });
        } else {
            requestContexts.set(xhr, {
                page: false,
                epoch: pageEpoch
            });
        }
        activeRequests.add(xhr);
    }

    function requestCanSwap(event) {
        const context = requestContexts.get(event.detail.xhr);
        if (context == null) {
            return true;
        }
        if (context.page) {
            return context.sequence === latestPageRequest;
        }
        return context.epoch === pageEpoch
            && event.detail.target instanceof Element
            && event.detail.target.isConnected;
    }

    function rejectStaleSwap(event) {
        if (requestCanSwap(event)) {
            return false;
        }

        event.detail.shouldSwap = false;
        event.preventDefault();
        return true;
    }

    function responseBodyClass(responseDocument, targetId) {
        const responseTarget = targetId == null
            ? null
            : responseDocument.getElementById(targetId);
        if (responseTarget?.hasAttribute("data-app-body-class")) {
            return responseTarget.dataset.appBodyClass ?? "";
        }
        return responseDocument.body?.getAttribute("class");
    }

    function applyCurrentPageBodyClass() {
        const pageTarget = document.getElementById("app-social-page-content")
            ?? document.getElementById("app-page-content");
        if (pageTarget?.hasAttribute("data-app-body-class")) {
            document.body.className = pageTarget.dataset.appBodyClass ?? "";
        }
    }

    function syncBodyClassFromResponse(event) {
        if (!isPageSwap(event)
                || event.detail.xhr?.responseText == null) {
            return;
        }

        const responseDocument = new DOMParser().parseFromString(
            event.detail.xhr.responseText,
            "text/html");
        const className = responseBodyClass(
            responseDocument,
            event.detail.target?.id);
        if (className != null) {
            document.body.className = className;
        }
        if (responseDocument.title) {
            document.title = responseDocument.title;
        }
        const responseUrl = new URL(
            event.detail.xhr.responseURL || window.location.href,
            window.location.href);
        storeBodyClass(
            responseUrl.pathname + responseUrl.search,
            document.body.className);
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

    function syncHistoryBodyClass(event) {
        const pageTarget = document.getElementById("app-social-page-content")
            ?? document.getElementById("app-page-content");
        const responseText = event.detail.serverResponse;
        let className = null;
        if (pageTarget != null && typeof responseText === "string") {
            const responseDocument = new DOMParser().parseFromString(
                responseText,
                "text/html");
            className = responseBodyClass(responseDocument, pageTarget.id);
        }
        if (className == null) {
            restoreBodyClass();
            className = document.body.className;
        } else {
            document.body.className = className;
            storeBodyClass(
                window.location.pathname + window.location.search,
                className);
        }
        if (pageTarget != null) {
            pageTarget.dataset.appBodyClass = className;
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
        registerRequest(event);
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

    document.addEventListener("htmx:beforeSwap", function (event) {
        if (rejectStaleSwap(event)) {
            return;
        }
        if (event.detail.shouldSwap) {
            syncBodyClassFromResponse(event);
        }
    });

    document.addEventListener("htmx:afterRequest", function (event) {
        activeRequests.delete(event.detail.xhr);
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

    document.addEventListener("htmx:historyCacheMiss", function (event) {
        pageEpoch += 1;
        latestPageRequest += 1;
        abortActiveRequests();
        if (event.detail.xhr instanceof XMLHttpRequest) {
            activeRequests.add(event.detail.xhr);
        }
    });

    document.addEventListener("htmx:historyRestore", function (event) {
        pageEpoch += 1;
        latestPageRequest += 1;
        abortActiveRequests();
        syncHistoryBodyClass(event);
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

        pageEpoch += 1;
        applyCurrentPageBodyClass();
        dispatchUiEvent("app-navigation-changed", {
            path: window.location.pathname
        });
        window.AppUi.refreshSidebar();
        closeMobileSidebar();
    });

    document.addEventListener("DOMContentLoaded", function () {
        applyCurrentPageBodyClass();
        storeBodyClass(
            window.location.pathname + window.location.search,
            document.body.className);
        dispatchUiEvent("app-navigation-changed", {
            path: window.location.pathname
        });
    });
})();
