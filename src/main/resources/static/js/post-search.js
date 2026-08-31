(function () {
    const CSRF_COOKIE_NAME = "XSRF-TOKEN";
    const CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    const SUPPORTED_POST_TYPES = new Set([
        "STANDARD",
        "SHORT",
        "VIDEO"
    ]);

    function readCookie(name) {
        const prefix = `${name}=`;
        const cookie = document.cookie
            .split("; ")
            .find(item => item.startsWith(prefix));
        return cookie == null
            ? null
            : decodeURIComponent(cookie.substring(prefix.length));
    }

    function asText(value, maxLength = 16000) {
        return typeof value === "string"
            ? value.substring(0, maxLength)
            : "";
    }

    function normalizeLocalPath(value) {
        if (typeof value !== "string"
                || !value.startsWith("/")
                || value.startsWith("//")) {
            return null;
        }
        return value.substring(0, 2000);
    }

    function normalizeMedia(value) {
        if (!Array.isArray(value)) {
            return [];
        }

        return value.slice(0, 20)
            .filter(media => media != null && typeof media === "object")
            .map(media => ({
                kind: asText(media.kind, 32).toUpperCase(),
                previewUrl: normalizeLocalPath(media.previewUrl)
            }));
    }

    function normalizeItem(value) {
        if (value == null || typeof value !== "object") {
            return null;
        }

        const relevance = Number(value.relevancePercent);
        return {
            content: asText(value.content, 6000),
            detailPath: normalizeLocalPath(value.detailPath),
            relevancePercent: Number.isFinite(relevance)
                ? Math.min(100, Math.max(0, Math.round(relevance)))
                : 0,
            media: normalizeMedia(value.media)
        };
    }

    function normalizeSections(value) {
        if (!Array.isArray(value)) {
            return [];
        }

        return value.slice(0, 3)
            .filter(section => section != null
                    && typeof section === "object")
            .map(section => {
                const postType = asText(
                    section.postType,
                    32).toUpperCase();
                const items = Array.isArray(section.items)
                    ? section.items.slice(0, 100)
                        .map(item => normalizeItem(item))
                        .filter(item => item != null)
                    : [];
                return {
                    postType,
                    title: asText(section.title, 200),
                    items
                };
            })
            .filter(section => SUPPORTED_POST_TYPES.has(section.postType)
                    && section.items.length > 0);
    }

    function initializeSearch(root) {
        const search = root.querySelector?.("[data-post-search]");
        if (search == null
                || search.dataset.postSearchInitialized === "true") {
            return;
        }
        search.dataset.postSearchInitialized = "true";

        const query = search.dataset.query?.trim() ?? "";
        const form = search.querySelector("[data-post-search-form]");
        const input = form.querySelector("input[name='q']");
        const loading = search.querySelector("[data-post-search-loading]");
        const status = search.querySelector("[data-post-search-status]");
        const results = search.querySelector("[data-post-search-results]");
        const sectionList = search.querySelector(
            "[data-post-search-section-list]");
        const sectionTemplate = search.querySelector(
            "[data-post-search-section-template]");
        const itemTemplates = {
            STANDARD: search.querySelector(
                "[data-post-search-standard-template]"),
            SHORT: search.querySelector(
                "[data-post-search-short-template]"),
            VIDEO: search.querySelector(
                "[data-post-search-video-template]")
        };
        let searchController = null;

        form.addEventListener("submit", event => {
            if (input.value.trim() === "") {
                event.preventDefault();
                input.focus();
            }
        });

        function resolveErrorMessage(response) {
            if (response.status === 401 || response.status === 403) {
                return search.dataset.authMessage;
            }
            if (response.status === 429) {
                return search.dataset.rateLimitMessage;
            }
            return search.dataset.errorMessage;
        }

        function requestHeaders() {
            const csrfToken = readCookie(CSRF_COOKIE_NAME);
            return {
                Accept: "application/json",
                "Content-Type":
                    "application/x-www-form-urlencoded;charset=UTF-8",
                ...(csrfToken
                    ? { [CSRF_HEADER_NAME]: csrfToken }
                    : {})
            };
        }

        function showStatus(message, danger = false) {
            status.textContent = message;
            status.classList.toggle("alert-danger", danger);
            status.classList.toggle("alert-secondary", !danger);
            status.hidden = false;
        }

        function setItemLink(link, detailPath) {
            if (detailPath != null) {
                link.href = detailPath;
                return;
            }

            link.classList.add("disabled");
            link.setAttribute("aria-disabled", "true");
        }

        function buildMediaFallback(kind) {
            const icon = document.createElement("i");
            icon.setAttribute("aria-hidden", "true");
            icon.className = resolveMediaIcon(kind);
            return icon;
        }

        function resolveMediaIcon(kind) {
            if (kind === "VIDEO") {
                return "bi bi-film";
            }
            if (kind === "AUDIO") {
                return "bi bi-music-note-beamed";
            }
            if (kind === "IMAGE") {
                return "bi bi-image";
            }
            return "bi bi-file-earmark";
        }

        function renderStandardMedia(container, media) {
            if (media.length === 0) {
                container.hidden = true;
                return;
            }

            const visibleMedia = media.slice(0, 4);
            container.dataset.mediaCount = String(visibleMedia.length);
            container.hidden = false;
            visibleMedia.forEach((asset, index) => {
                const item = document.createElement("span");
                item.className = "post-search-standard-media-item";

                if (asset.previewUrl != null) {
                    const image = document.createElement("img");
                    image.src = asset.previewUrl;
                    image.alt = "";
                    image.loading = "lazy";
                    item.append(image);
                } else {
                    item.classList.add(
                        "d-grid",
                        "text-body-secondary",
                        "align-items-center",
                        "justify-content-center");
                    item.append(buildMediaFallback(asset.kind));
                }

                if (index === visibleMedia.length - 1
                        && media.length > visibleMedia.length) {
                    const more = document.createElement("span");
                    more.className = "post-search-standard-media-more";
                    more.textContent =
                        `+${media.length - visibleMedia.length}`;
                    item.append(more);
                }
                container.append(item);
            });
        }

        function renderPrimaryMedia(link, media) {
            const image = link.querySelector(
                "[data-post-search-media-image]");
            const fallback = link.querySelector(
                "[data-post-search-media-fallback]");
            const primary = media[0];
            if (primary?.previewUrl == null) {
                return;
            }

            image.src = primary.previewUrl;
            image.hidden = false;
            fallback.hidden = true;
        }

        function renderItem(postType, item, grid) {
            const fragment = itemTemplates[postType]
                .content.cloneNode(true);
            const link = fragment.querySelector(
                "[data-post-search-item]");
            setItemLink(link, item.detailPath);
            link.querySelector("[data-post-search-content]")
                .textContent = item.content;
            link.querySelector("[data-post-search-relevance]")
                .textContent =
                    `${search.dataset.relevanceLabel} `
                    + `${item.relevancePercent}%`;

            if (postType === "STANDARD") {
                renderStandardMedia(
                    link.querySelector(
                        "[data-post-search-standard-media]"),
                    item.media);
            } else {
                renderPrimaryMedia(link, item.media);
            }
            grid.append(fragment);
        }

        function renderSections(value) {
            const sections = normalizeSections(value);
            sectionList.replaceChildren();
            let totalItems = 0;

            sections.forEach(section => {
                const fragment = sectionTemplate.content.cloneNode(true);
                fragment.querySelector(
                    "[data-post-search-section-title]")
                    .textContent = section.title;
                fragment.querySelector(
                    "[data-post-search-section-count]")
                    .textContent = String(section.items.length);
                const grid = fragment.querySelector(
                    "[data-post-search-section-grid]");
                section.items.forEach(item => {
                    renderItem(section.postType, item, grid);
                });
                sectionList.append(fragment);
                totalItems += section.items.length;
            });

            results.hidden = totalItems === 0;
            return totalItems;
        }

        function renderResults(payload) {
            loading.hidden = true;
            const totalItems = renderSections(payload?.sections);

            if (payload?.retrievalAvailability !== "READY") {
                showStatus(search.dataset.unavailableMessage, true);
                return;
            }
            if (totalItems === 0) {
                showStatus(search.dataset.noResultsMessage);
                return;
            }

            status.hidden = true;
        }

        async function runSearch() {
            if (searchController != null) {
                return;
            }

            searchController = new AbortController();
            loading.hidden = false;
            status.hidden = true;
            results.hidden = true;

            try {
                const response = await fetch(search.dataset.resultsPath, {
                    method: "POST",
                    credentials: "same-origin",
                    signal: searchController.signal,
                    headers: requestHeaders(),
                    body: new URLSearchParams({ query })
                });
                if (response.redirected || !response.ok) {
                    throw new SearchRequestError(
                        resolveErrorMessage(response));
                }
                if (!response.headers.get("Content-Type")
                    ?.includes("application/json")) {
                    throw new SearchRequestError(search.dataset.errorMessage);
                }

                renderResults(await response.json());
            } catch (error) {
                if (error?.name === "AbortError") {
                    return;
                }
                loading.hidden = true;
                results.hidden = true;
                showStatus(
                    error instanceof SearchRequestError && error.message
                        ? error.message
                        : search.dataset.errorMessage,
                    true);
            } finally {
                searchController = null;
            }
        }

        search.postSearchTeardown = () => {
            searchController?.abort();
            searchController = null;
        };

        if (query !== "") {
            runSearch();
        } else {
            input.focus();
        }
    }

    function teardownSearch(root) {
        const searches = [];
        if (root?.matches?.("[data-post-search]")) {
            searches.push(root);
        }
        root?.querySelectorAll?.("[data-post-search]")
            .forEach(search => searches.push(search));
        searches.forEach(search => search.postSearchTeardown?.());
    }

    class SearchRequestError extends Error {
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", () => {
            initializeSearch(document);
        });
    } else {
        initializeSearch(document);
    }

    document.addEventListener("htmx:afterSwap", event => {
        initializeSearch(event.detail.target ?? document);
    });
    document.addEventListener("htmx:beforeCleanupElement", event => {
        teardownSearch(event.detail?.elt ?? event.target);
    });
    window.addEventListener("pagehide", () => {
        teardownSearch(document);
    });
})();
