(function () {
    const THEME_STORAGE_KEY = "app-theme";
    const THEME_COOKIE_NAME = "APP_THEME";
    const CSRF_COOKIE_NAME = "XSRF-TOKEN";
    const CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    const SIDEBAR_STORAGE_KEY = "app-sidebar-collapsed";
    const root = document.documentElement;
    const desktopViewport = window.matchMedia("(min-width: 992px)");
    let sidebarTooltips = [];

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
                    darkThemeActive ? "Switch to light theme" : "Switch to dark theme");
                const icon = button.querySelector("[data-app-theme-icon]");
                icon.classList.toggle("bi-sun", darkThemeActive);
                icon.classList.toggle("bi-moon-stars", !darkThemeActive);
                button.querySelector("[data-app-theme-label]").textContent = darkThemeActive
                    ? "Light theme"
                    : "Dark theme";
            });
    }

    function showLoader() {
        document.getElementById("app-loader")?.removeAttribute("hidden");
    }

    function hideLoader() {
        document.getElementById("app-loader")?.setAttribute("hidden", "");
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
                    collapsed ? "Expand menu" : "Collapse menu");

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

    document.addEventListener("htmx:configRequest", function (event) {
        const csrfToken = readCookie(CSRF_COOKIE_NAME);
        if (csrfToken) {
            event.detail.headers[CSRF_HEADER_NAME] = csrfToken;
        }
    });

    document.addEventListener("htmx:beforeRequest", function (event) {
        if (event.detail.elt.closest("[data-app-loader='manual']") == null) {
            showLoader();
        }
    });
    document.addEventListener("htmx:afterRequest", hideLoader);
    document.addEventListener("htmx:sendError", hideLoader);
    document.addEventListener("htmx:responseError", hideLoader);

    document.addEventListener("DOMContentLoaded", function () {
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
            const sidebarToggle = event.target.closest(
                "[data-app-sidebar-toggle]");
            if (sidebarToggle != null) {
                applySidebarState(
                    root.dataset.appSidebar !== "collapsed");
                return;
            }

            const menuGroup = event.target.closest(".app-menu-toggle");
            if (menuGroup != null
                    && desktopViewport.matches
                    && root.dataset.appSidebar === "collapsed") {
                applySidebarState(false);
            }
        }, true);

        desktopViewport.addEventListener("change", function () {
            syncSidebarTooltips(isSidebarCollapsed());
        });

        document.addEventListener("submit", function (event) {
            const form = event.target;
            if (event.defaultPrevented
                    || form.matches("[data-app-loader='manual']")) {
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
                    || href.startsWith("#")
                    || href.startsWith("javascript:")) {
                return;
            }

            showLoader();
        });

        window.addEventListener("pageshow", hideLoader);
    });
})();
