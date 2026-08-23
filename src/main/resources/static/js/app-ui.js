(function () {
    const THEME_STORAGE_KEY = "app-theme";
    const THEME_COOKIE_NAME = "APP_THEME";
    const CSRF_COOKIE_NAME = "XSRF-TOKEN";
    const CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    const SIDEBAR_STORAGE_KEY = "app-sidebar-collapsed";
    const BODY_CLASS_STORAGE_PREFIX = "app-body-class:";
    const root = document.documentElement;
    const desktopViewport = window.matchMedia("(min-width: 992px)");
    let sidebarTooltips = [];
    const busyTargetCounts = new WeakMap();

    function getTheme() {
        const cookieTheme = readCookie(THEME_COOKIE_NAME);
        if (cookieTheme === "dark" || cookieTheme === "light") {
            localStorage.setItem(THEME_STORAGE_KEY, cookieTheme);
            return cookieTheme;
        }

        return localStorage.getItem(THEME_STORAGE_KEY) === "dark"
                ? "dark"
                : "light";
    }

    function applyTheme(theme) {
        root.setAttribute("data-bs-theme", theme);
        localStorage.setItem(THEME_STORAGE_KEY, theme);
    }

    function updateThemeButtons(theme) {
        const darkThemeActive = theme === "dark";

        document.querySelectorAll("[data-app-theme-toggle]")
            .forEach(button => {
                button.setAttribute(
                    "aria-label",
                    darkThemeActive
                        ? button.dataset.switchLightLabel
                        : button.dataset.switchDarkLabel);
                const icon = button.querySelector("[data-app-theme-icon]");
                icon.classList.toggle("bi-sun", darkThemeActive);
                icon.classList.toggle("bi-moon-stars", !darkThemeActive);
                button.querySelector("[data-app-theme-label]").textContent = darkThemeActive
                    ? button.dataset.lightLabel
                    : button.dataset.darkLabel;
            });
    }

    function showLoader() {
        document.getElementById("app-loader")?.removeAttribute("hidden");
    }

    function hideLoader() {
        document.getElementById("app-loader")?.setAttribute("hidden", "");
    }

    function isBoosted(element) {
        const boostRoot = element?.closest("[hx-boost]");
        return boostRoot != null
                && boostRoot.getAttribute("hx-boost") !== "false";
    }

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

    function syncBodyClassFromResponse(event) {
        if (!event.detail.boosted || event.detail.xhr?.responseText == null) {
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
        const responseUrl = new URL(
            event.detail.xhr.responseURL || window.location.href,
            window.location.href);
        storeBodyClass(
            responseUrl.pathname + responseUrl.search,
            responseBody.className);
    }

    function restoreBodyClass() {
        const className = sessionStorage.getItem(bodyClassStorageKey(
            window.location.pathname + window.location.search))
                ?? sessionStorage.getItem(bodyClassStorageKey(
                    window.location.pathname));
        if (className != null) {
            document.body.className = className;
        }
    }

    function hideHtmxError() {
        document.getElementById("app-htmx-error")
            ?.setAttribute("hidden", "");
    }

    function showHtmxError(messageType) {
        const alert = document.getElementById("app-htmx-error");
        if (alert == null) {
            return;
        }

        const message = messageType === "connection"
            ? alert.dataset.connectionMessage
            : alert.dataset.requestMessage;
        const messageElement = alert.querySelector(
            "[data-app-htmx-error-message]");
        if (messageElement != null) {
            messageElement.textContent = message;
        }
        alert.removeAttribute("hidden");
    }

    function isSidebarCollapsed() {
        return localStorage.getItem(SIDEBAR_STORAGE_KEY) === "true";
    }

    function disposeSidebarTooltips() {
        sidebarTooltips.forEach(tooltip => tooltip.dispose());
        sidebarTooltips = [];
    }

    function syncSidebarTooltips(collapsed) {
        disposeSidebarTooltips();

        if (!collapsed
                || !desktopViewport.matches
                || typeof bootstrap === "undefined") {
            return;
        }

        sidebarTooltips = Array.from(
            document.querySelectorAll("[data-app-menu-tooltip]"))
            .map(element => new bootstrap.Tooltip(element, {
                placement: "right",
                trigger: "hover focus",
                container: "body"
            }));
    }

    function applySidebarState(collapsed) {
        root.dataset.appSidebar = collapsed ? "collapsed" : "expanded";
        localStorage.setItem(SIDEBAR_STORAGE_KEY, String(collapsed));

        document.querySelectorAll("[data-app-sidebar-toggle]")
            .forEach(button => {
                button.setAttribute("aria-expanded", String(!collapsed));
                button.setAttribute(
                    "aria-label",
                    collapsed
                        ? button.dataset.expandLabel
                        : button.dataset.collapseLabel);

                const icon = button.querySelector(
                    "[data-app-sidebar-toggle-icon]");
                icon?.classList.toggle("bi-layout-sidebar-inset", !collapsed);
                icon?.classList.toggle("bi-layout-sidebar", collapsed);
            });

        syncSidebarTooltips(collapsed);
    }

    function readCookie(name) {
        const prefix = `${name}=`;
        const cookie = document.cookie
            .split("; ")
            .find(item => item.startsWith(prefix));

        return cookie == null
            ? null
            : decodeURIComponent(cookie.substring(prefix.length));
    }

    applyTheme(getTheme());
    root.dataset.appSidebar = isSidebarCollapsed()
        ? "collapsed"
        : "expanded";

    document.addEventListener("submit", function (event) {
        const form = event.target;
        if (!form.matches("[data-app-prune-empty-params]")) {
            return;
        }

        const emptyControls = Array.from(form.elements)
            .filter(control => control.name
                    && !control.disabled
                    && typeof control.value === "string"
                    && control.value.trim() === "");
        emptyControls.forEach(control => {
            control.disabled = true;
        });
        window.setTimeout(() => {
            emptyControls.forEach(control => {
                control.disabled = false;
            });
        }, 0);
    }, true);

    document.addEventListener("htmx:configRequest", function (event) {
        const csrfToken = readCookie(CSRF_COOKIE_NAME);
        if (csrfToken) {
            event.detail.headers[CSRF_HEADER_NAME] = csrfToken;
        }
    });

    document.addEventListener("htmx:beforeRequest", function (event) {
        hideHtmxError();
        if (event.detail.elt.closest("[data-app-loader='manual']") != null) {
            return;
        }

        setRequestTargetBusy(event.detail.target, true);
        if (event.detail.elt.closest("[data-app-loader='global']") != null) {
            showLoader();
        }
    });
    document.addEventListener("htmx:beforeSwap", syncBodyClassFromResponse);
    document.addEventListener("htmx:removingHeadElement", function (event) {
        if (event.detail.headElement instanceof HTMLScriptElement) {
            event.preventDefault();
        }
    });
    document.addEventListener("htmx:afterRequest", function (event) {
        setRequestTargetBusy(event.detail.target, false);
        hideLoader();
    });
    document.addEventListener("htmx:sendError", function (event) {
        setRequestTargetBusy(event.detail.target, false);
        hideLoader();
        showHtmxError("connection");
    });
    document.addEventListener("htmx:timeout", function (event) {
        setRequestTargetBusy(event.detail.target, false);
        hideLoader();
        showHtmxError("connection");
    });
    document.addEventListener("htmx:responseError", function (event) {
        setRequestTargetBusy(event.detail.target, false);
        hideLoader();
        showHtmxError("request");
    });
    document.addEventListener("htmx:historyRestore", restoreBodyClass);
    document.addEventListener("htmx:afterSwap", function (event) {
        if (!event.detail.boosted) {
            return;
        }

        updateThemeButtons(getTheme());
        applySidebarState(isSidebarCollapsed());
    });

    document.addEventListener("DOMContentLoaded", function () {
        storeBodyClass(
            window.location.pathname + window.location.search,
            document.body.className);
        updateThemeButtons(getTheme());
        applySidebarState(isSidebarCollapsed());

        document.querySelectorAll("[data-app-theme-toggle]")
            .forEach(button => {
                button.addEventListener("click", function () {
                    const currentTheme =
                        root.getAttribute("data-bs-theme") === "dark"
                            ? "dark"
                            : "light";
                    const theme = currentTheme === "dark" ? "light" : "dark";
                    applyTheme(theme);
                    updateThemeButtons(theme);

                    const form = button.closest("[data-app-theme-form]");
                    if (form?.dataset.appThemePersist === "true") {
                        form.querySelector("[data-app-theme-value]").value =
                            String(theme === "dark");
                        form.requestSubmit();
                    }
                });
            });

        document.addEventListener("click", function (event) {
            if (event.target.closest("[data-app-htmx-error-dismiss]") != null) {
                hideHtmxError();
                return;
            }

            const sidebarToggle = event.target.closest(
                "[data-app-sidebar-toggle]");
            if (sidebarToggle != null) {
                applySidebarState(
                    root.dataset.appSidebar !== "collapsed");
                return;
            }
        }, true);

        desktopViewport.addEventListener("change", function () {
            syncSidebarTooltips(isSidebarCollapsed());
        });

        document.addEventListener("submit", function (event) {
            const form = event.target;
            if (event.defaultPrevented
                    || form.matches("[data-app-loader='manual']")
                    || isBoosted(form)) {
                return;
            }

            showLoader();
        });

        document.addEventListener("click", function (event) {
            const link = event.target.closest("a[href]");
            if (link == null || event.defaultPrevented || event.button !== 0) {
                return;
            }

            const href = link.getAttribute("href");
            if (event.ctrlKey
                    || event.metaKey
                    || event.shiftKey
                    || event.altKey
                    || link.target === "_blank"
                    || link.hasAttribute("download")
                    || link.hasAttribute("hx-get")
                    || isBoosted(link)
                    || href.startsWith("#")
                    || href.startsWith("javascript:")) {
                return;
            }

            showLoader();
        });

        window.addEventListener("pageshow", hideLoader);
    });
})();
