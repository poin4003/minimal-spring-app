(function () {
    const THEME_STORAGE_KEY = "app-theme";
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

    applyTheme(getTheme());

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
