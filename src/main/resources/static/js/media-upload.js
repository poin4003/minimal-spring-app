import { MediaChunkUploadError, MediaChunkUploader } from "./media-chunk-upload.js";
import { MediaDirectUploader } from "./media-direct-upload.js";

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

    class MediaUploadResumeStore {
        constructor(prefix = "app.media.upload-session") {
            this.prefix = prefix;
        }

        find(file) {
            try {
                const value = localStorage.getItem(this.key(file));
                if (value == null) {
                    return null;
                }

                const session = JSON.parse(value);
                if (session.originalName !== file.name
                        || session.fileSize !== file.size
                        || session.lastModified !== file.lastModified
                        || typeof session.uploadId !== "string") {
                    this.remove(file);
                    return null;
                }
                return session.uploadId;
            } catch {
                this.remove(file);
                return null;
            }
        }

        save(file, uploadId) {
            try {
                localStorage.setItem(this.key(file), JSON.stringify({
                    uploadId,
                    originalName: file.name,
                    fileSize: file.size,
                    lastModified: file.lastModified
                }));
            } catch {
                // Upload continues without cross-page resume when storage is unavailable.
            }
        }

        remove(file) {
            try {
                localStorage.removeItem(this.key(file));
            } catch {
                // Nothing else is required when browser storage is unavailable.
            }
        }

        key(file) {
            return [
                this.prefix,
                encodeURIComponent(file.name),
                file.size,
                file.lastModified
            ].join(":");
        }
    }

    class MediaUploadQueue {
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
            this.transport = this.readTransport();
            this.resumeStore = new MediaUploadResumeStore();
            this.directUploader = this.createDirectUploader();
            this.chunkUploader = this.createChunkUploader();
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

        readTransport() {
            return {
                directUploadThresholdBytes: Number(
                    this.form.dataset.directUploadThreshold
                ),
                chunkUploadPath: this.form.dataset.chunkUploadPath,
                parallelChunks: Number(this.form.dataset.parallelChunks)
            };
        }

        createDirectUploader() {
            return new MediaDirectUploader({
                uploadUrl: this.form.action,
                headers: this.requestHeaders("text/html")
            });
        }

        createChunkUploader() {
            return new MediaChunkUploader({
                baseUrl: this.transport.chunkUploadPath,
                concurrency: this.transport.parallelChunks,
                requestHeadersProvider: () => this.requestHeaders("application/json")
            });
        }

        requestHeaders(accept) {
            const headers = {
                Accept: accept,
                "HX-Request": "true"
            };
            const csrfHeader = this.form.dataset.csrfHeader;
            const csrfToken = this.form.dataset.csrfToken;
            if (csrfHeader && csrfToken) {
                headers[csrfHeader] = csrfToken;
            }
            return headers;
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
            const chunked = file.size > this.transport.directUploadThresholdBytes;
            const uploadSessionId = chunked
                ? this.resumeStore.find(file)
                : null;
            const transportLabel = chunked
                ? uploadSessionId == null
                    ? "Chunked upload"
                    : "Resumable upload found"
                : "Direct upload";

            element.dataset.uploadId = id;
            element.querySelector("[data-upload-name]").textContent = file.name;
            element.querySelector("[data-upload-size]").textContent =
                `${this.formatBytes(file.size)} | ${transportLabel}`;

            return {
                id,
                file,
                element,
                chunked,
                uploadSessionId,
                status: STATUS.QUEUED,
                cancelOperation: null,
                cancelRequested: false
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
            item.cancelRequested = true;
            item.cancelOperation?.();

            if (item.chunked && item.uploadSessionId != null) {
                this.discardChunkSession(item);
            }

            if (item.status === STATUS.QUEUED) {
                this.setStatus(item, STATUS.CANCELLED);
                this.showLocalError(item, "Upload cancelled.");
                this.updateControls();
            }
        }

        retryItem(item) {
            const validationMessage = this.validate(item.file);
            if (validationMessage != null) {
                this.showLocalError(item, validationMessage);
                return;
            }

            item.cancelRequested = false;
            this.clearResult(item);
            this.setProgress(item, 0);
            this.setStatus(item, STATUS.QUEUED);
            this.updateControls();
            this.uploadQueued();
        }

        removeItem(item) {
            item.cancelRequested = true;
            item.cancelOperation?.();
            if (item.chunked && item.uploadSessionId != null) {
                this.discardChunkSession(item, false);
            }

            item.element.remove();
            this.items.delete(item.id);
            this.updateControls();
        }

        async discardChunkSession(item, showError = true) {
            const uploadSessionId = item.uploadSessionId;
            item.uploadSessionId = null;
            this.resumeStore.remove(item.file);

            try {
                await this.chunkUploader.cancel(uploadSessionId);
            } catch (error) {
                if (showError
                        && !(error instanceof MediaChunkUploadError
                            && error.error === "RESOURCE_NOT_FOUND")) {
                    this.showLocalError(
                        item,
                        error.message || "The upload session could not be cancelled."
                    );
                }
            }
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
            return item.chunked
                ? this.uploadChunked(item)
                : this.uploadDirect(item);
        }

        async uploadDirect(item) {
            item.cancelRequested = false;
            this.clearResult(item);
            this.setProgress(item, 0);
            this.setStatus(item, STATUS.UPLOADING);

            const operation = this.directUploader.upload(item.file, {
                onProgress: progress => this.setProgress(item, progress.percent)
            });
            item.cancelOperation = operation.cancel;

            try {
                const response = await operation.result;
                if (response.redirectPath) {
                    window.location.assign(response.redirectPath);
                    return;
                }

                this.showServerResult(item, response.html);
                if (response.ok) {
                    this.setProgress(item, 100);
                    this.setStatus(item, STATUS.SUCCESS);
                    this.dispatchUploaded(item);
                } else {
                    this.setStatus(item, STATUS.FAILED);
                }
            } catch (error) {
                if (error.name === "AbortError" || item.cancelRequested) {
                    this.setStatus(item, STATUS.CANCELLED);
                    this.showLocalError(item, "Upload cancelled.");
                } else {
                    this.setStatus(item, STATUS.FAILED);
                    this.showLocalError(item, error.message);
                }
            } finally {
                item.cancelOperation = null;
            }
        }

        async uploadChunked(item) {
            item.cancelRequested = false;
            this.clearResult(item);
            this.setProgress(item, 0);
            this.setStatus(item, STATUS.UPLOADING);

            const abortController = new AbortController();
            item.cancelOperation = () => abortController.abort();

            try {
                const media = await this.chunkUploader.upload(item.file, {
                    uploadId: item.uploadSessionId,
                    signal: abortController.signal,
                    onSession: session => {
                        item.uploadSessionId = session.id;
                        this.resumeStore.save(item.file, session.id);
                    },
                    onProgress: progress => this.setProgress(item, progress.percent)
                });

                item.uploadSessionId = null;
                this.resumeStore.remove(item.file);
                this.showChunkSuccess(item, media);
                this.setProgress(item, 100);
                this.setStatus(item, STATUS.SUCCESS);
                this.dispatchUploaded(item, media);
            } catch (error) {
                if (error.name === "AbortError" || item.cancelRequested) {
                    this.setStatus(item, STATUS.CANCELLED);
                    this.showLocalError(item, "Upload cancelled.");
                } else {
                    if (error instanceof MediaChunkUploadError
                            && (error.error === "UPLOAD_FILE_MISMATCH"
                                || error.error === "UPLOAD_SESSION_NOT_RESUMABLE")) {
                        await this.discardChunkSession(item, false);
                    }
                    this.setStatus(item, STATUS.FAILED);
                    this.showLocalError(
                        item,
                        error.message || "Chunk upload failed."
                    );
                }
            } finally {
                item.cancelOperation = null;
            }
        }

        dispatchUploaded(item, media = null) {
            const result = item.element.querySelector("[data-media-upload-result]");
            if (media == null && result == null) {
                return;
            }

            this.root.dispatchEvent(new CustomEvent("media:uploaded", {
                bubbles: true,
                detail: media == null
                    ? {
                        mediaId: result.dataset.mediaId,
                        originalName: result.dataset.originalName,
                        processingStatus: result.dataset.processingStatus,
                        thumbnailUrl: result.dataset.thumbnailUrl || null
                    }
                    : {
                        mediaId: media.id,
                        originalName: media.originalName,
                        processingStatus: media.processingStatus,
                        thumbnailUrl: media.thumbnailUrl || null
                    }
            }));
        }

        showChunkSuccess(item, media) {
            const result = item.element.querySelector("[data-upload-result]");
            const alert = document.createElement("div");
            const title = document.createElement("div");
            const fileName = document.createElement("div");

            alert.className = "alert alert-success mb-0";
            alert.setAttribute("role", "status");
            alert.setAttribute("data-media-upload-result", "");
            alert.dataset.mediaId = media.id;
            alert.dataset.originalName = media.originalName;
            alert.dataset.processingStatus = media.processingStatus;
            if (media.thumbnailUrl) {
                alert.dataset.thumbnailUrl = media.thumbnailUrl;
            }

            title.className = "fw-semibold";
            title.textContent = "Media uploaded successfully.";
            fileName.className = "small mt-1 text-break";
            fileName.textContent = media.originalName;
            alert.append(title, fileName);

            result.replaceChildren(alert);
            result.hidden = false;
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
            const normalizedPercent = Math.max(0, Math.min(100, percent));
            const progress = item.element.querySelector("[role='progressbar']");
            const progressBar = item.element.querySelector("[data-upload-progress]");
            progress.setAttribute("aria-valuenow", String(normalizedPercent));
            progressBar.style.width = `${normalizedPercent}%`;
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

    function initializeUploads(scope) {
        const roots = [];
        if (scope instanceof Element && scope.matches("[data-media-upload]")) {
            roots.push(scope);
        }
        if (scope instanceof Document || scope instanceof Element) {
            roots.push(...scope.querySelectorAll("[data-media-upload]"));
        }

        roots.forEach(root => {
            if (root.dataset.mediaUploadInitialized === "true") {
                return;
            }

            root.dataset.mediaUploadInitialized = "true";
            new MediaUploadQueue(root);
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        initializeUploads(document);
    });

    document.addEventListener("htmx:load", function (event) {
        initializeUploads(event.detail.elt);
    });
})();
