(function () {
    "use strict";

    const STATUS = Object.freeze({
        QUEUED: "QUEUED",
        UPLOADING: "UPLOADING",
        SUCCESS: "SUCCESS",
        FAILED: "FAILED",
        CANCELLED: "CANCELLED"
    });

    const STATUS_VIEW = Object.freeze({
        QUEUED: ["Queued", "text-bg-secondary"],
        UPLOADING: ["Uploading", "text-bg-primary"],
        SUCCESS: ["Uploaded", "text-bg-success"],
        FAILED: ["Failed", "text-bg-danger"],
        CANCELLED: ["Cancelled", "text-bg-warning"]
    });

    class DirectMediaUpload {
        constructor(root) {
            this.root = root;
            this.form = root.querySelector("[data-media-upload-form]");
            this.input = root.querySelector("[data-media-upload-input]");
            this.dropzone = root.querySelector("[data-media-upload-dropzone]");
            this.queue = root.querySelector("[data-media-upload-queue]");
            this.emptyState = root.querySelector("[data-media-upload-empty]");
            this.itemTemplate = root.querySelector("[data-media-upload-item-template]");
            this.startButton = root.querySelector("[data-media-upload-start]");
            this.clearButton = root.querySelector("[data-media-upload-clear]");
            this.rules = this.readRules();
            this.items = new Map();
            this.uploading = false;

            this.bindEvents();
            this.updateControls();
        }

        readRules() {
            return new Map(
                Array.from(this.root.querySelectorAll("[data-media-upload-rule]"))
                    .map(element => [
                        element.dataset.extension.toLowerCase(),
                        Number(element.dataset.maxFileSize)
                    ])
            );
        }

        bindEvents() {
            this.input.addEventListener("change", () => {
                this.addFiles(this.input.files);
                this.input.value = "";
            });

            this.dropzone.addEventListener("keydown", event => {
                if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    this.input.click();
                }
            });

            ["dragenter", "dragover"].forEach(eventName => {
                this.dropzone.addEventListener(eventName, event => {
                    event.preventDefault();
                    this.dropzone.classList.add("is-dragging");
                });
            });

            ["dragleave", "drop"].forEach(eventName => {
                this.dropzone.addEventListener(eventName, event => {
                    event.preventDefault();
                    this.dropzone.classList.remove("is-dragging");
                });
            });

            this.dropzone.addEventListener("drop", event => {
                this.addFiles(event.dataTransfer.files);
            });

            this.form.addEventListener("submit", event => {
                event.preventDefault();
                this.uploadQueued();
            });

            this.queue.addEventListener("click", event => {
                const button = event.target.closest("[data-upload-action]");
                if (button == null) {
                    return;
                }

                const itemElement = button.closest("[data-upload-item]");
                const item = this.items.get(itemElement?.dataset.uploadId);
                if (item == null) {
                    return;
                }

                this.handleAction(button.dataset.uploadAction, item);
            });

            this.clearButton.addEventListener("click", () => {
                Array.from(this.items.values())
                    .filter(item => item.status === STATUS.SUCCESS
                            || item.status === STATUS.CANCELLED)
                    .forEach(item => this.removeItem(item));
            });
        }

        addFiles(fileList) {
            Array.from(fileList).forEach(file => {
                const item = this.createItem(file);
                this.items.set(item.id, item);
                this.queue.append(item.element);

                const validationMessage = this.validate(file);
                if (validationMessage != null) {
                    this.setStatus(item, STATUS.FAILED);
                    this.showLocalError(item, validationMessage);
                }
            });

            this.updateControls();
        }

        createItem(file) {
            const element = this.itemTemplate.content.firstElementChild.cloneNode(true);
            const id = crypto.randomUUID();

            element.dataset.uploadId = id;
            element.querySelector("[data-upload-name]").textContent = file.name;
            element.querySelector("[data-upload-size]").textContent = this.formatBytes(file.size);

            return {
                id,
                file,
                element,
                status: STATUS.QUEUED,
                xhr: null
            };
        }

        validate(file) {
            if (file.size <= 0) {
                return "The selected file is empty.";
            }

            const extension = this.getExtension(file.name);
            const maxFileSize = this.rules.get(extension);
            if (maxFileSize == null) {
                return "This file extension is not allowed.";
            }
            if (file.size > maxFileSize) {
                return `The file exceeds the ${this.formatBytes(maxFileSize)} limit.`;
            }
            return null;
        }

        getExtension(fileName) {
            const separatorIndex = fileName.lastIndexOf(".");
            return separatorIndex < 0
                ? ""
                : fileName.slice(separatorIndex + 1).toLowerCase();
        }

        handleAction(action, item) {
            if (action === "cancel") {
                this.cancelItem(item);
                return;
            }
            if (action === "retry") {
                this.retryItem(item);
                return;
            }
            if (action === "remove") {
                this.removeItem(item);
            }
        }

        cancelItem(item) {
            if (item.status === STATUS.UPLOADING) {
                item.xhr?.abort();
                return;
            }
            if (item.status === STATUS.QUEUED) {
                this.setStatus(item, STATUS.CANCELLED);
                this.updateControls();
            }
        }

        retryItem(item) {
            const validationMessage = this.validate(item.file);
            if (validationMessage != null) {
                this.showLocalError(item, validationMessage);
                return;
            }

            this.clearResult(item);
            this.setProgress(item, 0);
            this.setStatus(item, STATUS.QUEUED);
            this.updateControls();
            this.uploadQueued();
        }

        removeItem(item) {
            item.status = STATUS.CANCELLED;
            item.xhr?.abort();
            item.element.remove();
            this.items.delete(item.id);
            this.updateControls();
        }

        async uploadQueued() {
            if (this.uploading) {
                return;
            }

            this.uploading = true;
            this.updateControls();
            try {
                const queuedItems = Array.from(this.items.values())
                    .filter(item => item.status === STATUS.QUEUED);

                for (const item of queuedItems) {
                    if (item.status === STATUS.QUEUED) {
                        await this.uploadItem(item);
                    }
                }
            } finally {
                this.uploading = false;
                this.updateControls();
            }
        }

        uploadItem(item) {
            return new Promise(resolve => {
                const xhr = new XMLHttpRequest();
                const formData = new FormData();
                formData.append("file", item.file, item.file.name);

                item.xhr = xhr;
                this.clearResult(item);
                this.setProgress(item, 0);
                this.setStatus(item, STATUS.UPLOADING);

                xhr.open("POST", this.form.action);
                xhr.withCredentials = true;
                xhr.setRequestHeader("Accept", "text/html");
                xhr.setRequestHeader("HX-Request", "true");
                xhr.setRequestHeader(
                    this.form.dataset.csrfHeader,
                    this.form.dataset.csrfToken
                );

                xhr.upload.addEventListener("progress", event => {
                    if (event.lengthComputable) {
                        this.setProgress(
                            item,
                            Math.round((event.loaded / event.total) * 100)
                        );
                    }
                });

                xhr.addEventListener("load", () => {
                    item.xhr = null;

                    const redirectPath = xhr.getResponseHeader("HX-Redirect");
                    if (redirectPath) {
                        window.location.assign(redirectPath);
                        resolve();
                        return;
                    }

                    this.showServerResult(item, xhr.responseText);
                    if (xhr.status >= 200 && xhr.status < 300) {
                        this.setProgress(item, 100);
                        this.setStatus(item, STATUS.SUCCESS);
                        this.dispatchUploaded(item);
                    } else {
                        this.setStatus(item, STATUS.FAILED);
                    }
                    resolve();
                });

                xhr.addEventListener("error", () => {
                    item.xhr = null;
                    this.setStatus(item, STATUS.FAILED);
                    this.showLocalError(item, "The upload request could not reach the server.");
                    resolve();
                });

                xhr.addEventListener("abort", () => {
                    item.xhr = null;
                    this.setStatus(item, STATUS.CANCELLED);
                    this.showLocalError(item, "Upload cancelled.");
                    resolve();
                });

                xhr.send(formData);
            });
        }

        dispatchUploaded(item) {
            const result = item.element.querySelector("[data-media-upload-result]");
            if (result == null) {
                return;
            }

            this.root.dispatchEvent(new CustomEvent("media:uploaded", {
                bubbles: true,
                detail: {
                    mediaId: result.dataset.mediaId,
                    originalName: result.dataset.originalName,
                    processingStatus: result.dataset.processingStatus,
                    thumbnailUrl: result.dataset.thumbnailUrl || null
                }
            }));
        }

        showServerResult(item, html) {
            const result = item.element.querySelector("[data-upload-result]");
            result.innerHTML = html;
            result.hidden = false;
        }

        showLocalError(item, message) {
            const result = item.element.querySelector("[data-upload-result]");
            const alert = document.createElement("div");
            alert.className = "alert alert-danger mb-0";
            alert.setAttribute("role", "alert");
            alert.textContent = message;
            result.replaceChildren(alert);
            result.hidden = false;
        }

        clearResult(item) {
            const result = item.element.querySelector("[data-upload-result]");
            result.replaceChildren();
            result.hidden = true;
        }

        setProgress(item, percent) {
            const progress = item.element.querySelector("[role='progressbar']");
            const progressBar = item.element.querySelector("[data-upload-progress]");
            progress.setAttribute("aria-valuenow", String(percent));
            progressBar.style.width = `${percent}%`;
        }

        setStatus(item, status) {
            item.status = status;

            const [label, badgeClass] = STATUS_VIEW[status];
            const badge = item.element.querySelector("[data-upload-status]");
            badge.className = `badge ${badgeClass}`;
            badge.textContent = label;

            const cancelButton = item.element.querySelector("[data-upload-action='cancel']");
            const retryButton = item.element.querySelector("[data-upload-action='retry']");
            const removeButton = item.element.querySelector("[data-upload-action='remove']");

            cancelButton.hidden = status !== STATUS.QUEUED
                    && status !== STATUS.UPLOADING;
            retryButton.hidden = status !== STATUS.FAILED
                    && status !== STATUS.CANCELLED;
            removeButton.hidden = status === STATUS.UPLOADING;
        }

        updateControls() {
            const items = Array.from(this.items.values());
            const hasQueued = items.some(item => item.status === STATUS.QUEUED);
            const hasCompleted = items.some(item => item.status === STATUS.SUCCESS
                    || item.status === STATUS.CANCELLED);

            this.emptyState.hidden = items.length > 0;
            this.startButton.disabled = this.uploading || !hasQueued;
            this.clearButton.disabled = this.uploading || !hasCompleted;
        }

        formatBytes(bytes) {
            if (bytes < 1024) {
                return `${bytes} B`;
            }

            const units = ["KB", "MB", "GB", "TB"];
            let value = bytes;
            let unitIndex = -1;
            while (value >= 1024 && unitIndex < units.length - 1) {
                value /= 1024;
                unitIndex++;
            }
            return `${value.toFixed(1)} ${units[unitIndex]}`;
        }
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("[data-media-upload]")
            .forEach(root => new DirectMediaUpload(root));
    });
})();
