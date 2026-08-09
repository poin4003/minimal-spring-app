(() => {
    const ROOT_SELECTOR = "[data-post-composer]";

    function initializeAll(scope = document) {
        if (scope.matches?.(ROOT_SELECTOR)) {
            initialize(scope);
        }
        scope.querySelectorAll?.(ROOT_SELECTOR).forEach(initialize);
    }

    function initialize(root) {
        if (root.postComposerState) {
            syncOptions(root, root.postComposerState);
            return;
        }

        const inputHost = root.querySelector("[data-selected-media-inputs]");
        const selected = Array.from(
            inputHost?.querySelectorAll("input[name='mediaIds']") || []
        ).map((input) => ({
            id: input.value,
            name: input.dataset.mediaName || input.value
        }));

        root.postComposerState = {
            selected,
            maxCount: Number(root.dataset.maxMediaCount || 20)
        };
        render(root, root.postComposerState);
    }

    function render(root, state) {
        const inputHost = root.querySelector("[data-selected-media-inputs]");
        const list = root.querySelector("[data-selected-media-list]");
        const empty = root.querySelector("[data-selected-media-empty]");
        const count = root.querySelector("[data-selected-media-count]");

        inputHost.replaceChildren();
        list.replaceChildren();
        count.textContent = `${state.selected.length}/${state.maxCount}`;
        empty.hidden = state.selected.length > 0;

        state.selected.forEach((media, index) => {
            inputHost.append(createHiddenInput(media));
            list.append(createSelectedItem(root, media, index, state));
        });

        syncOptions(root, state);
    }

    function createHiddenInput(media) {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = "mediaIds";
        input.value = media.id;
        input.dataset.mediaName = media.name;
        return input;
    }

    function createSelectedItem(root, media, index, state) {
        const item = document.createElement("div");
        const name = document.createElement("span");
        const actions = document.createElement("span");

        item.className = "list-group-item d-flex align-items-center gap-3";
        name.className = "flex-grow-1 text-truncate";
        name.textContent = media.name;
        name.title = media.name;
        actions.className = "btn-group btn-group-sm";
        actions.setAttribute("role", "group");

        actions.append(
            createActionButton(
                root.dataset.messageMoveUp,
                "up",
                media.id,
                "bi-arrow-up",
                index === 0
            ),
            createActionButton(
                root.dataset.messageMoveDown,
                "down",
                media.id,
                "bi-arrow-down",
                index === state.selected.length - 1
            ),
            createActionButton(
                root.dataset.messageRemove,
                "remove",
                media.id,
                "bi-x-lg",
                false,
                "btn-outline-danger"
            )
        );
        item.append(name, actions);
        return item;
    }

    function createActionButton(
        label,
        action,
        mediaId,
        iconClass,
        disabled,
        buttonClass = "btn-outline-secondary"
    ) {
        const button = document.createElement("button");
        const icon = document.createElement("i");

        button.type = "button";
        button.className = `btn ${buttonClass}`;
        button.dataset.selectedMediaAction = action;
        button.dataset.mediaId = mediaId;
        button.title = label;
        button.setAttribute("aria-label", label);
        button.disabled = disabled;
        icon.className = `bi ${iconClass}`;
        icon.setAttribute("aria-hidden", "true");
        button.append(icon);
        return button;
    }

    function syncOptions(root, state) {
        const selectedIds = new Set(state.selected.map((media) => media.id));
        root.querySelectorAll("[data-post-media-option]").forEach((option) => {
            const selected = selectedIds.has(option.dataset.mediaId);
            option.setAttribute("aria-pressed", String(selected));
            option.classList.toggle("border-primary", selected);
            option.classList.toggle("border-2", selected);
            option.querySelector("[data-media-selected-indicator]")
                ?.classList.toggle("d-none", !selected);
        });
    }

    function toggleOption(root, option) {
        const state = root.postComposerState;
        const mediaId = option.dataset.mediaId;
        const index = state.selected.findIndex((media) => media.id === mediaId);

        hideError(root);
        if (index >= 0) {
            state.selected.splice(index, 1);
        } else if (state.selected.length >= state.maxCount) {
            showError(root, root.dataset.messageLimit);
            return;
        } else {
            state.selected.push({
                id: mediaId,
                name: option.dataset.mediaName || mediaId
            });
        }
        render(root, state);
    }

    function applySelectedAction(root, button) {
        const state = root.postComposerState;
        const index = state.selected.findIndex(
            (media) => media.id === button.dataset.mediaId
        );
        if (index < 0) {
            return;
        }

        switch (button.dataset.selectedMediaAction) {
            case "up":
                if (index > 0) {
                    [state.selected[index - 1], state.selected[index]] =
                        [state.selected[index], state.selected[index - 1]];
                }
                break;
            case "down":
                if (index < state.selected.length - 1) {
                    [state.selected[index + 1], state.selected[index]] =
                        [state.selected[index], state.selected[index + 1]];
                }
                break;
            case "remove":
                state.selected.splice(index, 1);
                break;
            default:
                return;
        }
        hideError(root);
        render(root, state);
    }

    function showError(root, message) {
        const error = root.querySelector("[data-media-selection-error]");
        error.textContent = message;
        error.hidden = false;
    }

    function hideError(root) {
        const error = root.querySelector("[data-media-selection-error]");
        error.textContent = "";
        error.hidden = true;
    }

    document.addEventListener("click", (event) => {
        const option = event.target.closest("[data-post-media-option]");
        if (option) {
            const root = option.closest(ROOT_SELECTOR);
            initialize(root);
            toggleOption(root, option);
            return;
        }

        const action = event.target.closest("[data-selected-media-action]");
        if (action) {
            const root = action.closest(ROOT_SELECTOR);
            initialize(root);
            applySelectedAction(root, action);
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key !== "Enter"
            || !event.target.matches("[data-post-media-search]")) {
            return;
        }

        event.preventDefault();
        event.target.closest(ROOT_SELECTOR)
            ?.querySelector("[data-post-media-search-submit]")
            ?.click();
    });

    document.addEventListener("DOMContentLoaded", () => initializeAll());
    document.addEventListener("htmx:afterSwap", (event) => {
        initializeAll(event.target);
    });
})();
