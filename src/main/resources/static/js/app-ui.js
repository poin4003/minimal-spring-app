(function () {
    const THEME_STORAGE_KEY = "app-theme";
    const CSRF_COOKIE_NAME = "XSRF-TOKEN";
    const CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    const root = document.documentElement;

    function getTheme() {
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

        document.querySelectorAll("[data-app-theme-toggle]")
            .forEach(button => {
                button.addEventListener("click", function () {
                    const theme = getTheme() === "dark" ? "light" : "dark";
                    applyTheme(theme);
                    updateThemeButtons(theme);
                });
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
