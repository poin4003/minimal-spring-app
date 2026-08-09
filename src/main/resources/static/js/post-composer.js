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

        const selectedMediaControl = root.querySelector(
            "[data-selected-media-inputs]"
        );
        const selected = Array.from(
            selectedMediaControl?.selectedOptions || []
        ).map((option) => ({
            id: option.value,
            name: option.dataset.mediaName || option.textContent || option.value
        }));

        root.postComposerState = {
            selected,
            maxCount: Number(root.dataset.maxMediaCount || 20)
        };
        render(root, root.postComposerState);
    }

    function render(root, state) {
        const selectedMediaControl = root.querySelector(
            "[data-selected-media-inputs]"
        );

        selectedMediaControl.replaceChildren();

        state.selected.forEach((media) => {
            selectedMediaControl.append(createSelectedOption(media));
        });

        syncOptions(root, state);
    }

    function createSelectedOption(media) {
        const option = document.createElement("option");
        option.value = media.id;
        option.textContent = media.name;
        option.dataset.mediaName = media.name;
        option.selected = true;
        return option;
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
