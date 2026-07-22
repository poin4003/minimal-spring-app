(function () {
    document.addEventListener("error", function (event) {
        if (!(event.target instanceof HTMLImageElement)
                || !event.target.matches("[data-media-thumbnail]")) {
            return;
        }

        event.target.classList.add("d-none");
    }, true);
})();
