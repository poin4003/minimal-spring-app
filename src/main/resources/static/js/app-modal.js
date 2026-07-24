(function () {
    "use strict";

    const MODAL_HOST_ID = "app-modal-host";

    function isModalRequest(element) {
        return element instanceof Element
                && element.matches("[data-app-modal-request]");
    }

    function navigateToFallback(element) {
        if (!isModalRequest(element)) {
            return;
        }

        const fallbackPath = element.getAttribute("href");
        if (fallbackPath) {
            window.location.assign(fallbackPath);
        }
    }

    document.addEventListener("htmx:afterSwap", function (event) {
        const target = event.detail.target;
        if (!(target instanceof Element) || target.id !== MODAL_HOST_ID) {
            return;
        }

        const modalElement = target.querySelector(".modal");
        if (modalElement) {
            bootstrap.Modal.getOrCreateInstance(modalElement).show();
        }
    });

    document.addEventListener("htmx:beforeRequest", function (event) {
        const form = event.detail.elt;
        if (!(form instanceof HTMLFormElement)
                || !form.matches("[data-app-modal-form]")) {
            return;
        }

        const modalElement = form.closest(".modal");
        if (modalElement) {
            bootstrap.Modal.getInstance(modalElement)?.hide();
        }
    });

    document.addEventListener("htmx:responseError", function (event) {
        navigateToFallback(event.detail.elt);
    });

    document.addEventListener("htmx:sendError", function (event) {
        navigateToFallback(event.detail.elt);
    });

    document.addEventListener("hidden.bs.modal", function (event) {
        const modalElement = event.target;
        const modalHost = document.getElementById(MODAL_HOST_ID);
        if (!(modalElement instanceof Element)
                || modalHost == null
                || !modalHost.contains(modalElement)) {
            return;
        }

        bootstrap.Modal.getInstance(modalElement)?.dispose();
        modalHost.replaceChildren();
    });
})();
