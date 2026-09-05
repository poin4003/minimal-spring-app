(function () {
    "use strict";

    if (window.AppUi != null) {
        return;
    }

    const THEME_STORAGE_KEY = "app-theme";
    const THEME_COOKIE_NAME = "APP_THEME";
    const SIDEBAR_STORAGE_KEY = "app-sidebar-collapsed";
    const root = document.documentElement;
    const desktopViewport = window.matchMedia("(min-width: 992px)");
    let sidebarTooltips = [];

    function readCookie(name) {
        const prefix = `${name}=`;
        const cookie = document.cookie
            .split("; ")
            .find(item => item.startsWith(prefix));

        return cookie == null
            ? null
            : decodeURIComponent(cookie.substring(prefix.length));
    }

    function resolveTheme() {
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

    function resolveSidebarCollapsed() {
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
        syncSidebarTooltips(collapsed);
    }

    function refreshSidebar() {
        const sidebarStore = window.Alpine?.store("sidebar");
        applySidebarState(
            sidebarStore?.collapsed ?? resolveSidebarCollapsed());
    }

    function showLoader() {
        const loadingStore = window.Alpine?.store("loading");
        if (loadingStore != null) {
            loadingStore.show();
            return;
        }
        document.getElementById("app-loader")?.removeAttribute("hidden");
    }

    function hideLoader() {
        const loadingStore = window.Alpine?.store("loading");
        if (loadingStore != null) {
            loadingStore.hide();
            return;
        }
        document.getElementById("app-loader")?.setAttribute("hidden", "");
    }

    function showModal(modalElement) {
        if (!(modalElement instanceof Element)) {
            return;
        }

        const open = () => {
            if (typeof bootstrap !== "undefined") {
                bootstrap.Modal.getOrCreateInstance(modalElement).show();
            }
        };

        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", open, { once: true });
            return;
        }
        open();
    }

    function isBoosted(element) {
        const boostRoot = element?.closest("[hx-boost]");
        return boostRoot != null
            && boostRoot.getAttribute("hx-boost") !== "false";
    }

    function normalizeNavigationPath(path) {
        if (path == null || path === "") {
            return "/";
        }

        const normalized = path.split("?")[0].replace(/\/+$/, "");
        return normalized === "" ? "/" : normalized;
    }

    function modalRequestFallback(element) {
        if (!(element instanceof Element)
                || !element.matches("[data-app-modal-request]")) {
            return;
        }

        const fallbackPath = element.getAttribute("href");
        if (fallbackPath) {
            window.location.replace(fallbackPath);
        }
    }

    applyTheme(resolveTheme());
    root.dataset.appSidebar = resolveSidebarCollapsed()
        ? "collapsed"
        : "expanded";

    document.addEventListener("alpine:init", function () {
        Alpine.store("theme", {
            value: resolveTheme(),

            get dark() {
                return this.value === "dark";
            },

            toggle() {
                this.value = this.dark ? "light" : "dark";
                applyTheme(this.value);
            }
        });

        Alpine.store("sidebar", {
            collapsed: resolveSidebarCollapsed(),

            toggle() {
                this.collapsed = !this.collapsed;
                applySidebarState(this.collapsed);
            }
        });

        Alpine.store("loading", {
            visible: false,

            show() {
                this.visible = true;
            },

            hide() {
                this.visible = false;
            }
        });

        Alpine.store("navigation", {
            currentPath: normalizeNavigationPath(window.location.pathname),

            setPath(path) {
                this.currentPath = normalizeNavigationPath(path);
            },

            matches(path) {
                const itemPath = normalizeNavigationPath(path);
                return this.currentPath === itemPath
                    || this.currentPath.startsWith(itemPath + "/");
            }
        });

        Alpine.data("navigationShell", () => ({
            init() {
                this.update(window.location.pathname);
            },

            update(path) {
                Alpine.store("navigation").setPath(path);
            }
        }));

        Alpine.data("navigationItem", () => ({
            get active() {
                const itemPath = this.$root.dataset.appMenuPath;
                if (itemPath == null) {
                    return false;
                }

                const navigation = Alpine.store("navigation");
                if (!navigation.matches(itemPath)) {
                    return false;
                }
                const longestMatch = Array.from(document.querySelectorAll(
                    "[data-app-menu-path]"
                )).map(link => normalizeNavigationPath(
                    link.dataset.appMenuPath
                ))
                    .filter(path => navigation.matches(path))
                    .sort((left, right) => right.length - left.length)[0];
                return normalizeNavigationPath(itemPath) === longestMatch;
            }
        }));

        Alpine.data("navigationBranch", () => ({
            get active() {
                return Array.from(this.$root.querySelectorAll(
                    "[data-app-menu-path]"
                )).some(link => Alpine.store("navigation").matches(
                    link.dataset.appMenuPath));
            }
        }));

        Alpine.data("themeToggle", () => ({
            get dark() {
                return Alpine.store("theme").dark;
            },

            get toggleLabel() {
                return this.dark
                    ? this.$root.dataset.switchLightLabel
                    : this.$root.dataset.switchDarkLabel;
            },

            get themeLabel() {
                return this.dark
                    ? this.$root.dataset.lightLabel
                    : this.$root.dataset.darkLabel;
            },

            toggle() {
                Alpine.store("theme").toggle();
                if (this.$root.dataset.appThemePersist !== "true") {
                    return;
                }

                this.$refs.themeValue.value = String(this.dark);
                this.$nextTick(() => this.$root.requestSubmit());
            }
        }));

        Alpine.data("requestError", () => ({
            visible: false,
            message: "",

            show(type) {
                this.message = type === "connection"
                    ? this.$root.dataset.connectionMessage
                    : this.$root.dataset.requestMessage;
                this.visible = true;
            },

            hide() {
                this.visible = false;
            }
        }));

        Alpine.data("postComposer", () => ({
            selected: [],
            maxCount: 20,
            errorMessage: "",

            init() {
                this.maxCount = Number(
                    this.$root.dataset.maxMediaCount || 20);
                const control = this.$root.querySelector(
                    "[data-selected-media-inputs]");
                this.selected = Array.from(control?.selectedOptions || [])
                    .map(option => ({
                        id: option.value,
                        name: option.dataset.mediaName
                            || option.textContent
                            || option.value
                    }));
            },

            isSelected(mediaId) {
                return this.selected.some(media => media.id === mediaId);
            },

            toggleMedia(option) {
                const mediaId = option.dataset.mediaId;
                const selectedIndex = this.selected.findIndex(
                    media => media.id === mediaId);
                this.errorMessage = "";

                if (selectedIndex >= 0) {
                    this.selected.splice(selectedIndex, 1);
                } else if (this.maxCount === 1) {
                    this.selected = [this.toMedia(option)];
                } else if (this.selected.length >= this.maxCount) {
                    this.errorMessage = this.$root.dataset.messageLimit;
                    return;
                } else {
                    this.selected.push(this.toMedia(option));
                }

                this.syncSelectedControl();
            },

            toMedia(option) {
                return {
                    id: option.dataset.mediaId,
                    name: option.dataset.mediaName || option.dataset.mediaId
                };
            },

            syncSelectedControl() {
                const control = this.$root.querySelector(
                    "[data-selected-media-inputs]");
                if (control == null) {
                    return;
                }

                control.replaceChildren(...this.selected.map(media => {
                    const option = document.createElement("option");
                    option.value = media.id;
                    option.textContent = media.name;
                    option.dataset.mediaName = media.name;
                    option.selected = true;
                    return option;
                }));
            }
        }));

        Alpine.data("autoModal", () => ({
            init() {
                const modalId = this.$root.dataset.modalId;
                if (modalId != null) {
                    showModal(document.getElementById(modalId));
                }
            }
        }));

        Alpine.data("serverModalHost", () => ({
            hiddenHandler: null,

            init() {
                this.hiddenHandler = event => {
                    const modalElement = event.target;
                    if (!(modalElement instanceof Element)
                            || !this.$root.contains(modalElement)) {
                        return;
                    }

                    bootstrap.Modal.getInstance(modalElement)?.dispose();
                    this.$root.replaceChildren();
                };
                this.$root.addEventListener(
                    "hidden.bs.modal",
                    this.hiddenHandler);
            },

            loaded(event) {
                if (event.detail.target !== this.$root) {
                    return;
                }
                showModal(this.$root.querySelector(".modal"));
            },

            requestComplete(event) {
                const form = event.detail.element;
                if (!(form instanceof HTMLFormElement)
                        || !this.$root.contains(form)
                        || !event.detail.successful) {
                    return;
                }

                const modalElement = form.closest(".modal");
                if (modalElement) {
                    bootstrap.Modal.getOrCreateInstance(modalElement).hide();
                }
            },

            requestFailed(event) {
                modalRequestFallback(event.detail.element);
            },

            destroy() {
                this.$root.removeEventListener(
                    "hidden.bs.modal",
                    this.hiddenHandler);
            }
        }));

        Alpine.data("searchForm", () => ({
            query: "",

            init() {
                this.query = this.$root.dataset.initialQuery || "";
                if (this.query.trim() !== "") {
                    this.$nextTick(() => this.$refs.form.requestSubmit());
                }
            },

            submit(event) {
                this.query = this.query.trim();
                if (this.query !== "") {
                    return;
                }

                event.preventDefault();
                this.$refs.query.focus();
            }
        }));
    });

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

    document.addEventListener("DOMContentLoaded", function () {
        refreshSidebar();

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
        desktopViewport.addEventListener("change", refreshSidebar);
    });

    window.AppUi = Object.freeze({
        hideLoader,
        readCookie,
        refreshSidebar,
        showLoader,
        showModal
    });
})();
