(function () {
    const CSRF_COOKIE_NAME = "XSRF-TOKEN";
    const CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    const EVENT_BOUNDARY = /\r?\n\r?\n/;
    const SESSION_SCHEMA_VERSION = 1;
    const MAX_STORED_MESSAGES = 20;
    const PERSIST_DELAY_MS = 250;
    const ROLE_USER = "USER";
    const ROLE_ASSISTANT = "ASSISTANT";
    const STATUS_STREAMING = "STREAMING";
    const STATUS_COMPLETED = "COMPLETED";
    const STATUS_INTERRUPTED = "INTERRUPTED";
    const STATUS_ERROR = "ERROR";

    function readCookie(name) {
        const prefix = `${name}=`;
        const cookie = document.cookie
            .split("; ")
            .find(item => item.startsWith(prefix));

        return cookie == null
            ? null
            : decodeURIComponent(cookie.substring(prefix.length));
    }

    function createMessageId() {
        if (typeof globalThis.crypto?.randomUUID === "function") {
            return globalThis.crypto.randomUUID();
        }
        return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
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

    function normalizeSources(value) {
        if (!Array.isArray(value)) {
            return [];
        }

        return value.slice(0, 10)
            .filter(source => source != null
                && typeof source === "object")
            .map((source, index) => ({
                rank: Number.isFinite(Number(source.rank))
                    ? Number(source.rank)
                    : index + 1,
                postTypeLabel: asText(source.postTypeLabel, 100),
                relevancePercent: Number.isFinite(
                    Number(source.relevancePercent))
                    ? Number(source.relevancePercent)
                    : 0,
                content: asText(source.content, 4000),
                detailPath: normalizeDetailPath(source.detailPath)
            }));
    }

    function normalizeMessage(value) {
        if (value == null || typeof value !== "object") {
            return null;
        }

        const role = value.role === ROLE_USER
            ? ROLE_USER
            : value.role === ROLE_ASSISTANT
                ? ROLE_ASSISTANT
                : null;
        if (role == null) {
            return null;
        }

        const validStatuses = new Set([
            STATUS_STREAMING,
            STATUS_COMPLETED,
            STATUS_INTERRUPTED,
            STATUS_ERROR
        ]);
        const status = role === ROLE_USER
            ? STATUS_COMPLETED
            : validStatuses.has(value.status)
                ? value.status
                : STATUS_COMPLETED;

        return {
            id: asText(value.id, 100) || createMessageId(),
            role,
            content: asText(value.content),
            status,
            generated: role === ROLE_ASSISTANT
                && value.generated === true,
            sources: role === ROLE_ASSISTANT
                ? normalizeSources(value.sources)
                : []
        };
    }

    class ChatSessionStore {

        constructor(storageKey, maxMessages) {
            this.storageKey = storageKey;
            this.maxMessages = maxMessages;
            this.messages = [];
            this.persistTimer = null;
            this.load();
        }

        load() {
            if (!this.storageKey) {
                return;
            }

            try {
                const rawSnapshot = sessionStorage.getItem(this.storageKey);
                if (rawSnapshot == null) {
                    return;
                }

                const snapshot = JSON.parse(rawSnapshot);
                if (snapshot?.schemaVersion !== SESSION_SCHEMA_VERSION
                        || !Array.isArray(snapshot.messages)) {
                    return;
                }

                this.messages = snapshot.messages
                    .map(message => normalizeMessage(message))
                    .filter(message => message != null);
                this.trim();
            } catch (ignored) {
                this.messages = [];
            }
        }

        getMessages() {
            return this.messages;
        }

        append(message) {
            const normalized = normalizeMessage(message);
            if (normalized == null) {
                return null;
            }

            this.messages.push(normalized);
            this.trim();
            return normalized;
        }

        buildHistory(maxMessages) {
            const history = this.messages
                .filter(message => message.content.trim() !== "")
                .filter(message => message.role === ROLE_USER
                    || (message.status === STATUS_COMPLETED
                        && message.generated))
                .map(message => ({
                    role: message.role,
                    content: asText(
                        message.content,
                        message.role === ROLE_USER ? 2000 : 8000)
                }))
                .slice(-maxMessages);

            while (history[0]?.role === ROLE_ASSISTANT) {
                history.shift();
            }
            return history;
        }

        markStreamingInterrupted(defaultMessage) {
            let changed = false;
            this.messages.forEach(message => {
                if (message.role !== ROLE_ASSISTANT
                        || message.status !== STATUS_STREAMING) {
                    return;
                }

                message.status = STATUS_INTERRUPTED;
                if (message.content.trim() === "") {
                    message.content = defaultMessage;
                }
                changed = true;
            });

            if (changed) {
                this.persistNow();
            }
        }

        schedulePersist() {
            if (this.persistTimer != null) {
                return;
            }

            this.persistTimer = window.setTimeout(() => {
                this.persistTimer = null;
                this.persistNow();
            }, PERSIST_DELAY_MS);
        }

        persistNow() {
            if (this.persistTimer != null) {
                window.clearTimeout(this.persistTimer);
                this.persistTimer = null;
            }
            if (!this.storageKey) {
                return;
            }

            try {
                sessionStorage.setItem(this.storageKey, JSON.stringify({
                    schemaVersion: SESSION_SCHEMA_VERSION,
                    messages: this.messages
                }));
            } catch (ignored) {
                // Keep the conversation in memory when storage is unavailable.
            }
        }

        trim() {
            if (this.messages.length > this.maxMessages) {
                this.messages = this.messages.slice(-this.maxMessages);
            }
            if (this.messages[0]?.role === ROLE_ASSISTANT) {
                this.messages.shift();
            }
        }
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
            throw new ChatRequestError();
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

    function initializeChat(root) {
        const chat = root.querySelector?.("[data-ai-chat]");
        if (chat == null || chat.dataset.aiChatInitialized === "true") {
            return;
        }
        chat.dataset.aiChatInitialized = "true";

        const form = chat.querySelector("[data-ai-chat-form]");
        const input = chat.querySelector("[data-ai-chat-input]");
        const sendButton = chat.querySelector("[data-ai-chat-send]");
        const messages = chat.querySelector("[data-ai-chat-messages]");
        const thinking = chat.querySelector("[data-ai-chat-thinking]");
        const welcome = chat.querySelector("[data-ai-chat-welcome]");
        const userTemplate = chat.querySelector(
            "[data-ai-chat-user-template]");
        const assistantTemplate = chat.querySelector(
            "[data-ai-chat-assistant-template]");
        const sourceTemplate = chat.querySelector(
            "[data-ai-chat-source-template]");
        const configuredHistoryLimit = Number.parseInt(
            chat.dataset.maxHistoryMessages,
            10);
        const maxHistoryMessages = Number.isFinite(configuredHistoryLimit)
            ? Math.max(1, configuredHistoryLimit)
            : 6;
        const store = new ChatSessionStore(
            chat.dataset.sessionStorageKey,
            MAX_STORED_MESSAGES);
        let activeRequest = null;
        let scrollScheduled = false;

        function resizeInput() {
            input.style.height = "auto";
            input.style.height = `${Math.min(input.scrollHeight, 160)}px`;
        }

        function scrollToLatest(smooth = true) {
            if (scrollScheduled) {
                return;
            }
            scrollScheduled = true;
            window.requestAnimationFrame(() => {
                scrollScheduled = false;
                messages.scrollTo({
                    top: messages.scrollHeight,
                    behavior: smooth ? "smooth" : "auto"
                });
            });
        }

        function appendUserMessage(question) {
            const fragment = userTemplate.content.cloneNode(true);
            fragment.querySelector("[data-ai-chat-user-content]")
                .textContent = question;
            messages.append(fragment);
        }

        function createAssistantMessage() {
            const fragment = assistantTemplate.content.cloneNode(true);
            const article = fragment.querySelector(
                "[data-ai-chat-assistant-message]");
            const state = {
                article,
                answer: article.querySelector("[data-ai-chat-answer]"),
                mode: article.querySelector("[data-ai-chat-mode]"),
                status: article.querySelector("[data-ai-chat-status]"),
                sources: article.querySelector("[data-ai-chat-sources]"),
                sourceCount: article.querySelector(
                    "[data-ai-chat-source-count]"),
                sourceList: article.querySelector(
                    "[data-ai-chat-source-list]")
            };
            messages.append(fragment);
            return state;
        }

        function resolveErrorMessage(response) {
            if (response.status === 401 || response.status === 403) {
                return chat.dataset.authMessage;
            }
            if (response.status === 429) {
                return chat.dataset.rateLimitMessage;
            }
            return chat.dataset.errorMessage;
        }

        function setThinking(visible) {
            thinking.hidden = !visible;
        }

        function setBusy(busy) {
            input.disabled = busy;
            sendButton.disabled = busy;
            form.setAttribute("aria-busy", String(busy));
            if (!busy) {
                setThinking(false);
            }
        }

        function renderMode(state, generated) {
            state.mode.textContent = generated
                ? state.mode.dataset.generatedLabel
                : state.mode.dataset.searchLabel;
            state.mode.classList.toggle("text-bg-success", generated);
            state.mode.classList.toggle("text-bg-secondary", !generated);
        }

        function renderSourceItems(state, value) {
            const sources = normalizeSources(value);
            state.sourceList.replaceChildren();
            state.sourceCount.textContent = String(sources.length);
            state.sources.hidden = sources.length === 0;

            sources.forEach(source => {
                const fragment = sourceTemplate.content.cloneNode(true);
                const link = fragment.querySelector(
                    "[data-ai-chat-source]");
                link.querySelector("[data-ai-chat-source-rank]")
                    .textContent = String(source.rank);
                link.querySelector("[data-ai-chat-source-type]")
                    .textContent = source.postTypeLabel;
                link.querySelector("[data-ai-chat-source-relevance]")
                    .textContent = chat.dataset.relevanceTemplate.replace(
                        "0",
                        String(source.relevancePercent));
                link.querySelector("[data-ai-chat-source-content]")
                    .textContent = source.content;

                if (source.detailPath != null) {
                    link.href = source.detailPath;
                } else {
                    link.classList.add("disabled");
                    link.setAttribute("aria-disabled", "true");
                    link.removeAttribute("href");
                    link.querySelector(
                        "[data-ai-chat-source-link-icon]")?.remove();
                }
                state.sourceList.append(fragment);
            });

            return sources;
        }

        function renderSources(state, payload) {
            const sources = renderSourceItems(state, payload.sources);
            const willGenerate = payload.retrievalAvailability === "READY"
                && payload.generationAvailability === "READY"
                && sources.length > 0;
            renderMode(state, willGenerate);
            return { sources, willGenerate };
        }

        function appendToken(state, token) {
            state.answer.hidden = false;
            state.answer.append(document.createTextNode(token));
            renderMode(state, true);
            setThinking(false);
            scrollToLatest();
        }

        function renderCompletion(state, payload) {
            const generated = payload.generated === true;
            renderMode(state, generated);
            if (!generated || state.answer.textContent.trim() === "") {
                state.answer.replaceChildren(document.createTextNode(
                    payload.answer || chat.dataset.errorMessage));
                state.answer.hidden = false;
            }

            state.status.hidden = true;
            setThinking(false);
        }

        function renderStreamError(state, message) {
            renderMode(state, false);
            state.article.classList.add("border-danger-subtle");
            state.answer.classList.add("text-danger-emphasis");
            state.answer.replaceChildren(document.createTextNode(message));
            state.answer.hidden = false;
            state.status.hidden = true;
            setThinking(false);
        }

        function renderInterrupted(state, message) {
            renderMode(state, false);
            state.article.classList.add("border-warning-subtle");
            state.status.textContent = state.status.dataset.interruptedLabel;
            state.status.hidden = false;
            if (state.answer.textContent.trim() === "") {
                state.answer.replaceChildren(document.createTextNode(message));
            }
            state.answer.hidden = false;
            setThinking(false);
        }

        function renderStoredAssistant(message) {
            const state = createAssistantMessage();
            renderMode(state, message.generated);
            renderSourceItems(state, message.sources);

            if (message.content !== "") {
                state.answer.textContent = message.content;
                state.answer.hidden = false;
            }
            if (message.status === STATUS_INTERRUPTED) {
                renderInterrupted(state, chat.dataset.interruptedMessage);
            } else if (message.status === STATUS_ERROR) {
                renderStreamError(
                    state,
                    message.content || chat.dataset.errorMessage);
            }
        }

        function restoreConversation() {
            store.markStreamingInterrupted(
                chat.dataset.interruptedMessage);
            store.persistNow();
            const storedMessages = store.getMessages();
            if (storedMessages.length === 0) {
                return;
            }

            welcome?.remove();
            storedMessages.forEach(message => {
                if (message.role === ROLE_USER) {
                    appendUserMessage(message.content);
                } else {
                    renderStoredAssistant(message);
                }
            });
            scrollToLatest(false);
        }

        function parseJsonEvent(event) {
            try {
                return JSON.parse(event.data);
            } catch (error) {
                throw new ChatRequestError(chat.dataset.errorMessage);
            }
        }

        async function submitQuestion() {
            const question = input.value.trim();
            if (question === ""
                    || form.getAttribute("aria-busy") === "true") {
                return;
            }

            const history = store.buildHistory(maxHistoryMessages);
            welcome?.remove();
            appendUserMessage(question);
            store.append({
                id: createMessageId(),
                role: ROLE_USER,
                content: question,
                status: STATUS_COMPLETED
            });
            store.persistNow();
            input.value = "";
            resizeInput();
            setBusy(true);
            setThinking(true);
            scrollToLatest();

            const formData = new FormData(form);
            formData.set("question", question);
            history.forEach((message, index) => {
                formData.set(
                    `history[${index}].role`,
                    message.role);
                formData.set(
                    `history[${index}].content`,
                    message.content);
            });

            const csrfToken = readCookie(CSRF_COOKIE_NAME);
            const requestController = new AbortController();
            let assistantState = null;
            let assistantMessage = null;
            let completed = false;
            let settled = false;

            const ensureAssistant = () => {
                if (assistantState == null) {
                    assistantState = createAssistantMessage();
                }
                if (assistantMessage == null) {
                    assistantMessage = store.append({
                        id: createMessageId(),
                        role: ROLE_ASSISTANT,
                        content: "",
                        status: STATUS_STREAMING,
                        generated: false,
                        sources: []
                    });
                    store.schedulePersist();
                }
                return assistantState;
            };

            const interrupt = () => {
                if (settled) {
                    return;
                }

                const state = ensureAssistant();
                assistantMessage.status = STATUS_INTERRUPTED;
                if (assistantMessage.content.trim() === "") {
                    assistantMessage.content =
                        chat.dataset.interruptedMessage;
                }
                renderInterrupted(
                    state,
                    chat.dataset.interruptedMessage);
                settled = true;
                store.persistNow();
            };

            const requestState = {
                controller: requestController,
                interrupt
            };
            activeRequest = requestState;

            try {
                const response = await fetch(form.action, {
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
                    body: new URLSearchParams(formData)
                });
                if (response.redirected) {
                    throw new ChatRequestError(chat.dataset.authMessage);
                }
                if (!response.ok) {
                    throw new ChatRequestError(
                        resolveErrorMessage(response));
                }
                if (!response.headers.get("Content-Type")
                    ?.includes("text/event-stream")) {
                    throw new ChatRequestError(chat.dataset.errorMessage);
                }

                await consumeEventStream(response, event => {
                    if (event.name === "connected") {
                        return;
                    }
                    if (event.name === "sources") {
                        const sourceResult = renderSources(
                            ensureAssistant(),
                            parseJsonEvent(event));
                        assistantMessage.sources = sourceResult.sources;
                        assistantMessage.generated =
                            sourceResult.willGenerate;
                        store.schedulePersist();
                        scrollToLatest();
                        return;
                    }
                    if (event.name === "token") {
                        const payload = parseJsonEvent(event);
                        const token = payload.text || "";
                        appendToken(ensureAssistant(), token);
                        assistantMessage.content += token;
                        assistantMessage.generated = true;
                        store.schedulePersist();
                        return;
                    }
                    if (event.name === "complete") {
                        const payload = parseJsonEvent(event);
                        const state = ensureAssistant();
                        renderCompletion(state, payload);
                        assistantMessage.status = STATUS_COMPLETED;
                        assistantMessage.generated =
                            payload.generated === true;
                        assistantMessage.content = state.answer.textContent;
                        completed = true;
                        settled = true;
                        store.persistNow();
                        return;
                    }
                    if (event.name === "error") {
                        const payload = parseJsonEvent(event);
                        const message = payload.message
                            || chat.dataset.errorMessage;
                        renderStreamError(ensureAssistant(), message);
                        assistantMessage.status = STATUS_ERROR;
                        assistantMessage.generated = false;
                        assistantMessage.content = message;
                        settled = true;
                        store.persistNow();
                        throw new ChatRequestError(message, true);
                    }
                });

                if (!completed) {
                    throw new ChatRequestError(chat.dataset.errorMessage);
                }
            } catch (error) {
                if (error?.name === "AbortError") {
                    return;
                }

                const message = error instanceof ChatRequestError
                    && error.message
                    ? error.message
                    : chat.dataset.errorMessage;
                if (!(error instanceof ChatRequestError && error.rendered)) {
                    renderStreamError(ensureAssistant(), message);
                    assistantMessage.status = STATUS_ERROR;
                    assistantMessage.generated = false;
                    assistantMessage.content = message;
                    settled = true;
                    store.persistNow();
                }
            } finally {
                if (activeRequest === requestState) {
                    activeRequest = null;
                }
                setBusy(false);
                if (chat.isConnected) {
                    input.focus();
                    scrollToLatest();
                }
            }
        }

        form.addEventListener("submit", event => {
            event.preventDefault();
            submitQuestion();
        });
        input.addEventListener("input", resizeInput);
        input.addEventListener("keydown", event => {
            if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                form.requestSubmit();
            }
        });

        chat.aiChatTeardown = () => {
            if (activeRequest != null) {
                activeRequest.interrupt();
                activeRequest.controller.abort();
                activeRequest = null;
            }
            setBusy(false);
            store.persistNow();
        };

        restoreConversation();
        resizeInput();
        input.focus();
    }

    function teardownChat(root) {
        const chats = [];
        if (root?.matches?.("[data-ai-chat]")) {
            chats.push(root);
        }
        root?.querySelectorAll?.("[data-ai-chat]")
            .forEach(chat => chats.push(chat));
        chats.forEach(chat => chat.aiChatTeardown?.());
    }

    class ChatRequestError extends Error {

        constructor(message = "", rendered = false) {
            super(message);
            this.rendered = rendered;
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", () => {
            initializeChat(document);
        });
    } else {
        initializeChat(document);
    }

    document.addEventListener("htmx:afterSwap", event => {
        initializeChat(event.detail.target ?? document);
    });
    document.addEventListener("htmx:beforeCleanupElement", event => {
        teardownChat(event.detail?.elt ?? event.target);
    });
    window.addEventListener("pagehide", () => {
        teardownChat(document);
    });
})();
