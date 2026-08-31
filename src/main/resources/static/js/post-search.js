(function () {
    const CSRF_COOKIE_NAME = "XSRF-TOKEN";
    const CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    const EVENT_BOUNDARY = /\r?\n\r?\n/;

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

    function normalizeDetailPath(value) {
        if (typeof value !== "string"
                || !value.startsWith("/")
                || value.startsWith("//")) {
            return null;
        }
        return value.substring(0, 2000);
    }

    function normalizeItems(value) {
        if (!Array.isArray(value)) {
            return [];
        }

        return value.slice(0, 100)
            .filter(item => item != null && typeof item === "object")
            .map((item, index) => ({
                rank: Number.isFinite(Number(item.rank))
                    ? Number(item.rank)
                    : index + 1,
                postTypeLabel: asText(item.postTypeLabel, 100),
                content: asText(item.content, 6000),
                detailPath: normalizeDetailPath(item.detailPath)
            }));
    }

    function parseSseEvent(block) {
        let eventName = "message";
        const dataLines = [];

        block.split(/\r?\n/).forEach(line => {
            if (line === "" || line.startsWith(":")) {
                return;
            }

            const separator = line.indexOf(":");
            const field = separator < 0
                ? line
                : line.substring(0, separator);
            let value = separator < 0
                ? ""
                : line.substring(separator + 1);
            if (value.startsWith(" ")) {
                value = value.substring(1);
            }

            if (field === "event") {
                eventName = value;
            } else if (field === "data") {
                dataLines.push(value);
            }
        });

        return dataLines.length === 0
            ? null
            : { name: eventName, data: dataLines.join("\n") };
    }

    function dispatchBufferedEvents(buffer, eventHandler) {
        let boundary = EVENT_BOUNDARY.exec(buffer);
        while (boundary != null) {
            const block = buffer.substring(0, boundary.index);
            buffer = buffer.substring(
                boundary.index + boundary[0].length);
            const event = parseSseEvent(block);
            if (event != null) {
                eventHandler(event);
            }
            boundary = EVENT_BOUNDARY.exec(buffer);
        }
        return buffer;
    }

    async function consumeEventStream(response, eventHandler) {
        if (response.body == null) {
            throw new SearchRequestError();
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";

        try {
            while (true) {
                const chunk = await reader.read();
                if (chunk.done) {
                    buffer += decoder.decode();
                    break;
                }
                buffer += decoder.decode(chunk.value, { stream: true });
                buffer = dispatchBufferedEvents(buffer, eventHandler);
            }

            if (buffer.trim() !== "") {
                const event = parseSseEvent(buffer);
                if (event != null) {
                    eventHandler(event);
                }
            }
        } catch (error) {
            try {
                await reader.cancel();
            } catch (ignored) {
                // The stream may already be closed by the server.
            }
            throw error;
        } finally {
            reader.releaseLock();
        }
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
        const resultCount = search.querySelector(
            "[data-post-search-result-count]");
        const resultList = search.querySelector(
            "[data-post-search-result-list]");
        const itemTemplate = search.querySelector(
            "[data-post-search-item-template]");
        const summary = search.querySelector("[data-post-search-summary]");
        const summaryLoading = search.querySelector(
            "[data-post-search-summary-loading]");
        const summaryText = search.querySelector(
            "[data-post-search-summary-text]");
        const summaryTrigger = search.querySelector(
            "[data-post-search-summary-trigger]");
        let requestController = null;

        form.addEventListener("submit", event => {
            if (input.value.trim() === "") {
                event.preventDefault();
                input.focus();
            }
        });

        function parseJsonEvent(event) {
            try {
                return JSON.parse(event.data);
            } catch (error) {
                throw new SearchRequestError(search.dataset.errorMessage);
            }
        }

        function resolveErrorMessage(response) {
            if (response.status === 401 || response.status === 403) {
                return search.dataset.authMessage;
            }
            if (response.status === 429) {
                return search.dataset.rateLimitMessage;
            }
            return search.dataset.errorMessage;
        }

        function showStatus(message, danger = false) {
            status.textContent = message;
            status.classList.toggle("alert-danger", danger);
            status.classList.toggle("alert-secondary", !danger);
            status.hidden = false;
        }

        function hideLoading() {
            loading.hidden = true;
        }

        function renderItems(value) {
            const items = normalizeItems(value);
            resultList.replaceChildren();
            resultCount.textContent = String(items.length);
            results.hidden = items.length === 0;

            items.forEach(item => {
                const fragment = itemTemplate.content.cloneNode(true);
                const link = fragment.querySelector(
                    "[data-post-search-item]");
                link.querySelector("[data-post-search-rank]")
                    .textContent = String(item.rank);
                link.querySelector("[data-post-search-type]")
                    .textContent = item.postTypeLabel;
                link.querySelector("[data-post-search-content]")
                    .textContent = item.content;

                if (item.detailPath != null) {
                    link.href = item.detailPath;
                } else {
                    link.classList.add("disabled");
                    link.setAttribute("aria-disabled", "true");
                    link.querySelector(
                        "[data-post-search-link-icon]")?.remove();
                }
                resultList.append(fragment);
            });
            return items;
        }

        function renderResults(payload, summarize) {
            hideLoading();
            const items = renderItems(payload.items);
            if (payload.retrievalAvailability !== "READY") {
                summaryTrigger.hidden = true;
                showStatus(search.dataset.unavailableMessage, true);
                return;
            }
            if (items.length === 0) {
                summaryTrigger.hidden = true;
                showStatus(search.dataset.noResultsMessage);
                return;
            }

            status.hidden = true;
            const summaryReady = payload.summaryAvailability === "READY";
            summaryTrigger.hidden = !summaryReady || summarize;
            summaryTrigger.disabled = requestController != null;
            if (summaryReady && summarize) {
                summary.hidden = false;
                summaryLoading.hidden = false;
            }
        }

        function appendToken(token) {
            summary.hidden = false;
            summaryLoading.hidden = true;
            summaryText.append(document.createTextNode(token));
        }

        function renderCompletion(payload, summarize) {
            hideLoading();
            summaryLoading.hidden = true;
            if (payload.summarized !== true) {
                summary.hidden = true;
                summaryText.replaceChildren();
                if (summarize) {
                    summaryTrigger.hidden = false;
                }
            }
        }

        async function runSearch(summarize) {
            if (requestController != null) {
                return;
            }

            requestController = new AbortController();
            let receivedResults = false;
            let completed = false;
            const csrfToken = readCookie(CSRF_COOKIE_NAME);
            const body = new URLSearchParams({
                query,
                summarize: String(summarize)
            });
            if (summarize) {
                summary.hidden = false;
                summaryLoading.hidden = false;
                summaryText.replaceChildren();
                summaryTrigger.hidden = true;
            }

            try {
                const response = await fetch(search.dataset.streamPath, {
                    method: "POST",
                    credentials: "same-origin",
                    signal: requestController.signal,
                    headers: {
                        Accept: "text/event-stream",
                        "Content-Type":
                            "application/x-www-form-urlencoded;charset=UTF-8",
                        ...(csrfToken
                            ? { [CSRF_HEADER_NAME]: csrfToken }
                            : {})
                    },
                    body
                });
                if (response.redirected || !response.ok) {
                    throw new SearchRequestError(
                        resolveErrorMessage(response));
                }
                if (!response.headers.get("Content-Type")
                    ?.includes("text/event-stream")) {
                    throw new SearchRequestError(search.dataset.errorMessage);
                }

                await consumeEventStream(response, event => {
                    if (event.name === "connected") {
                        return;
                    }
                    if (event.name === "results") {
                        receivedResults = true;
                        renderResults(parseJsonEvent(event), summarize);
                        return;
                    }
                    if (event.name === "token") {
                        appendToken(parseJsonEvent(event).text || "");
                        return;
                    }
                    if (event.name === "complete") {
                        completed = true;
                        renderCompletion(parseJsonEvent(event), summarize);
                        return;
                    }
                    if (event.name === "error") {
                        throw new SearchRequestError(
                            parseJsonEvent(event).message
                                || search.dataset.errorMessage);
                    }
                });

                if (!completed) {
                    throw new SearchRequestError(search.dataset.errorMessage);
                }
            } catch (error) {
                if (error?.name === "AbortError") {
                    return;
                }
                hideLoading();
                summary.hidden = true;
                summaryLoading.hidden = true;
                if (summarize && receivedResults) {
                    summaryTrigger.hidden = false;
                }
                showStatus(
                    error instanceof SearchRequestError && error.message
                        ? error.message
                        : search.dataset.errorMessage,
                    true);
                if (!receivedResults) {
                    results.hidden = true;
                }
            } finally {
                requestController = null;
                summaryTrigger.disabled = false;
            }
        }

        summaryTrigger.addEventListener("click", () => {
            runSearch(true);
        });

        search.postSearchTeardown = () => {
            requestController?.abort();
            requestController = null;
        };

        if (query !== "") {
            runSearch(false);
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
