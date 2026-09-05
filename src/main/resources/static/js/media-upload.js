(function () {
    "use strict";

    if (window.AppMediaUploadInitialized === true) {
        return;
    }
    window.AppMediaUploadInitialized = true;

    const CSRF_COOKIE_NAME = "XSRF-TOKEN";
    const STATUS = Object.freeze({
        QUEUED: "QUEUED",
        UPLOADING: "UPLOADING",
        SUCCESS: "SUCCESS",
        FAILED: "FAILED",
        CANCELLED: "CANCELLED"
    });
    const STATUS_CLASS = Object.freeze({
        QUEUED: "text-bg-secondary",
        UPLOADING: "text-bg-primary",
        SUCCESS: "text-bg-success",
        FAILED: "text-bg-danger",
        CANCELLED: "text-bg-warning"
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
            } catch (_error) {
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
            } catch (_error) {
                // Upload can continue without cross-page resume.
            }
        }

        remove(file) {
            try {
                localStorage.removeItem(this.key(file));
            } catch (_error) {
                // Browser storage is optional.
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

    document.addEventListener("alpine:init", function () {
        Alpine.data("mediaUpload", () => ({
            items: [],
            uploading: false,
            dragging: false,
            rules: new Map(),
            messages: null,
            transport: null,
            resumeStore: new MediaUploadResumeStore(),
            directUploader: null,
            chunkUploader: null,

            init() {
                this.messages = this.readMessages();
                this.rules = new Map(Array.from(this.$root.querySelectorAll(
                    "[data-media-upload-rule]"
                )).map(element => [
                    element.dataset.extension.toLowerCase(),
                    Number(element.dataset.maxFileSize)
                ]));
                this.transport = {
                    directUploadThresholdBytes: Number(
                        this.$refs.form.dataset.directUploadThreshold),
                    chunkUploadPath:
                        this.$refs.form.dataset.chunkUploadPath,
                    parallelChunks: Number(
                        this.$refs.form.dataset.parallelChunks)
                };
                this.directUploader = new window.MediaDirectUploader({
                    uploadUrl: this.$refs.form.action,
                    requestHeadersProvider: () =>
                        this.requestHeaders("text/html"),
                    messages: {
                        requestFailed: this.messages.requestFailed,
                        cancelled: this.messages.uploadCancelled
                    }
                });
                this.chunkUploader = new window.MediaChunkUploader({
                    baseUrl: this.transport.chunkUploadPath,
                    concurrency: this.transport.parallelChunks,
                    requestHeadersProvider: () =>
                        this.requestHeaders("application/json"),
                    messages: {
                        authRequired: this.messages.authRequired,
                        requestFailed: this.messages.uploadFailed,
                        sessionNotResumable:
                            this.messages.sessionNotResumable,
                        fileMismatch: this.messages.fileMismatch
                    }
                });
            },

            readMessages() {
                return {
                    status: {
                        QUEUED: this.$root.dataset.messageStatusQueued,
                        UPLOADING:
                            this.$root.dataset.messageStatusUploading,
                        SUCCESS: this.$root.dataset.messageStatusUploaded,
                        FAILED: this.$root.dataset.messageStatusFailed,
                        CANCELLED:
                            this.$root.dataset.messageStatusCancelled
                    },
                    transportChunked:
                        this.$root.dataset.messageTransportChunked,
                    transportResumable:
                        this.$root.dataset.messageTransportResumable,
                    transportDirect:
                        this.$root.dataset.messageTransportDirect,
                    fileEmpty: this.$root.dataset.messageFileEmpty,
                    extensionNotAllowed:
                        this.$root.dataset.messageExtensionNotAllowed,
                    fileTooLarge: this.$root.dataset.messageFileTooLarge,
                    uploadCancelled:
                        this.$root.dataset.messageUploadCancelled,
                    sessionCancelFailed:
                        this.$root.dataset.messageSessionCancelFailed,
                    chunkFailed: this.$root.dataset.messageChunkFailed,
                    uploadSuccess: this.$root.dataset.messageUploadSuccess,
                    authExpired: this.$root.dataset.messageAuthExpired,
                    uploadFailed: this.$root.dataset.messageUploadFailed,
                    requestFailed: this.$root.dataset.messageRequestFailed,
                    authRequired: this.$root.dataset.messageAuthRequired,
                    sessionNotResumable:
                        this.$root.dataset.messageSessionNotResumable,
                    fileMismatch: this.$root.dataset.messageFileMismatch
                };
            },

            get hasQueued() {
                return this.items.some(item => item.status === STATUS.QUEUED);
            },

            get hasCompleted() {
                return this.items.some(item => item.status === STATUS.SUCCESS
                    || item.status === STATUS.CANCELLED);
            },

            statusClass(item) {
                return STATUS_CLASS[item.status];
            },

            statusLabel(item) {
                return this.messages.status[item.status];
            },

            requestHeaders(accept) {
                const headers = { Accept: accept, "HX-Request": "true" };
                const csrfHeader = this.$refs.form.dataset.csrfHeader;
                const csrfToken = window.AppUi.readCookie(CSRF_COOKIE_NAME)
                    || this.$refs.form.dataset.csrfToken;
                if (csrfHeader && csrfToken) {
                    headers[csrfHeader] = csrfToken;
                }
                return headers;
            },

            selectFiles(event) {
                this.addFiles(event.target.files);
                event.target.value = "";
            },

            dropFiles(event) {
                this.dragging = false;
                this.addFiles(event.dataTransfer.files);
            },

            addFiles(fileList) {
                Array.from(fileList).forEach(file => {
                    const chunked = file.size
                        > this.transport.directUploadThresholdBytes;
                    const uploadSessionId = chunked
                        ? this.resumeStore.find(file)
                        : null;
                    const transportLabel = chunked
                        ? uploadSessionId == null
                            ? this.messages.transportChunked
                            : this.messages.transportResumable
                        : this.messages.transportDirect;
                    const item = {
                        id: crypto.randomUUID(),
                        file,
                        name: file.name,
                        sizeLabel: `${this.formatBytes(file.size)} | `
                            + transportLabel,
                        chunked,
                        uploadSessionId,
                        status: STATUS.QUEUED,
                        progress: 0,
                        resultHtml: "",
                        resultMessage: "",
                        resultDanger: false,
                        cancelOperation: null,
                        cancelRequested: false
                    };
                    const validationMessage = this.validate(file);
                    if (validationMessage != null) {
                        item.status = STATUS.FAILED;
                        this.showLocalResult(
                            item,
                            validationMessage,
                            true);
                    }
                    this.items.push(item);
                });
            },

            validate(file) {
                if (file.size <= 0) {
                    return this.messages.fileEmpty;
                }
                const extension = file.name.includes(".")
                    ? file.name.slice(file.name.lastIndexOf(".") + 1)
                        .toLowerCase()
                    : "";
                const maxFileSize = this.rules.get(extension);
                if (maxFileSize == null) {
                    return this.messages.extensionNotAllowed;
                }
                return file.size > maxFileSize
                    ? this.formatMessage(
                        this.messages.fileTooLarge,
                        this.formatBytes(maxFileSize))
                    : null;
            },

            async uploadQueued() {
                if (this.uploading) {
                    return;
                }
                this.uploading = true;
                try {
                    const queuedItems = this.items.filter(
                        item => item.status === STATUS.QUEUED);
                    for (const item of queuedItems) {
                        if (item.status === STATUS.QUEUED) {
                            await (item.chunked
                                ? this.uploadChunked(item)
                                : this.uploadDirect(item));
                        }
                    }
                } finally {
                    this.uploading = false;
                }
            },

            resetForUpload(item) {
                item.cancelRequested = false;
                item.progress = 0;
                item.resultHtml = "";
                item.resultMessage = "";
                item.resultDanger = false;
                item.status = STATUS.UPLOADING;
            },

            async uploadDirect(item) {
                this.resetForUpload(item);
                const operation = this.directUploader.upload(item.file, {
                    onProgress: progress => {
                        item.progress = progress.percent;
                    }
                });
                item.cancelOperation = operation.cancel;
                try {
                    const response = await operation.result;
                    if (response.redirectPath) {
                        window.location.replace(response.redirectPath);
                        return;
                    }
                    if (!response.ok) {
                        item.status = STATUS.FAILED;
                        this.showRequestFailure(item, response);
                        return;
                    }
                    item.resultHtml = response.html;
                    item.progress = 100;
                    item.status = STATUS.SUCCESS;
                    this.dispatchUploaded(
                        this.mediaFromServerHtml(response.html));
                } catch (error) {
                    this.handleUploadError(item, error);
                } finally {
                    item.cancelOperation = null;
                }
            },

            async uploadChunked(item) {
                this.resetForUpload(item);
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
                        onProgress: progress => {
                            item.progress = progress.percent;
                        }
                    });
                    item.uploadSessionId = null;
                    this.resumeStore.remove(item.file);
                    item.progress = 100;
                    item.status = STATUS.SUCCESS;
                    this.showLocalResult(
                        item,
                        `${this.messages.uploadSuccess} ${media.originalName}`,
                        false);
                    this.dispatchUploaded(media);
                } catch (error) {
                    if (error instanceof window.MediaChunkUploadError
                            && (error.error === "UPLOAD_FILE_MISMATCH"
                                || error.error
                                    === "UPLOAD_SESSION_NOT_RESUMABLE")) {
                        await this.discardChunkSession(item, false);
                    }
                    this.handleUploadError(item, error);
                } finally {
                    item.cancelOperation = null;
                }
            },

            handleUploadError(item, error) {
                if (error?.name === "AbortError" || item.cancelRequested) {
                    item.status = STATUS.CANCELLED;
                    this.showLocalResult(
                        item,
                        this.messages.uploadCancelled,
                        true);
                    return;
                }
                item.status = STATUS.FAILED;
                this.showLocalResult(
                    item,
                    error?.message || this.messages.chunkFailed,
                    true);
            },

            showRequestFailure(item, response) {
                const template = document.createElement("template");
                template.innerHTML = response.html.trim();
                const alert = template.content.querySelector(
                    "[data-ui-error-alert]");
                if (alert != null) {
                    item.resultHtml = alert.outerHTML;
                    return;
                }
                this.showLocalResult(
                    item,
                    response.status === 403
                        ? this.messages.authExpired
                        : this.messages.uploadFailed,
                    true);
            },

            mediaFromServerHtml(html) {
                const template = document.createElement("template");
                template.innerHTML = html.trim();
                const result = template.content.querySelector(
                    "[data-media-upload-result]");
                return result == null ? null : {
                    mediaId: result.dataset.mediaId,
                    originalName: result.dataset.originalName,
                    processingStatus: result.dataset.processingStatus,
                    thumbnailUrl: result.dataset.thumbnailUrl || null
                };
            },

            dispatchUploaded(media) {
                if (media == null) {
                    return;
                }
                this.$root.dispatchEvent(new CustomEvent("media:uploaded", {
                    bubbles: true,
                    detail: media.id == null ? media : {
                        mediaId: media.id,
                        originalName: media.originalName,
                        processingStatus: media.processingStatus,
                        thumbnailUrl: media.thumbnailUrl || null
                    }
                }));
            },

            showLocalResult(item, message, danger) {
                item.resultHtml = "";
                item.resultMessage = message;
                item.resultDanger = danger;
            },

            cancelItem(item) {
                item.cancelRequested = true;
                item.cancelOperation?.();
                if (item.chunked && item.uploadSessionId != null) {
                    this.discardChunkSession(item);
                }
                if (item.status === STATUS.QUEUED) {
                    item.status = STATUS.CANCELLED;
                    this.showLocalResult(
                        item,
                        this.messages.uploadCancelled,
                        true);
                }
            },

            retryItem(item) {
                const validationMessage = this.validate(item.file);
                if (validationMessage != null) {
                    this.showLocalResult(item, validationMessage, true);
                    return;
                }
                item.cancelRequested = false;
                item.progress = 0;
                item.resultHtml = "";
                item.resultMessage = "";
                item.status = STATUS.QUEUED;
                this.uploadQueued();
            },

            removeItem(item) {
                item.cancelRequested = true;
                item.cancelOperation?.();
                if (item.chunked && item.uploadSessionId != null) {
                    this.discardChunkSession(item, false);
                }
                this.items = this.items.filter(current => current !== item);
            },

            clearCompleted() {
                this.items.filter(item => item.status === STATUS.SUCCESS
                    || item.status === STATUS.CANCELLED)
                    .forEach(item => this.removeItem(item));
            },

            async discardChunkSession(item, showError = true) {
                const uploadSessionId = item.uploadSessionId;
                item.uploadSessionId = null;
                this.resumeStore.remove(item.file);
                if (uploadSessionId == null) {
                    return;
                }
                try {
                    await this.chunkUploader.cancel(uploadSessionId);
                } catch (error) {
                    if (showError
                            && !(error
                                instanceof window.MediaChunkUploadError
                                && error.error === "RESOURCE_NOT_FOUND")) {
                        this.showLocalResult(
                            item,
                            error.message
                                || this.messages.sessionCancelFailed,
                            true);
                    }
                }
            },

            formatBytes(bytes) {
                if (bytes < 1024) {
                    return `${bytes} B`;
                }
                const units = ["KB", "MB", "GB", "TB"];
                let value = bytes;
                let unitIndex = -1;
                while (value >= 1024 && unitIndex < units.length - 1) {
                    value /= 1024;
                    unitIndex += 1;
                }
                return `${value.toFixed(1)} ${units[unitIndex]}`;
            },

            formatMessage(template, ...values) {
                return values.reduce((message, value, index) =>
                    message.replace(`{${index}}`, String(value)), template);
            },

            destroy() {
                this.items.forEach(item => {
                    item.cancelRequested = true;
                    item.cancelOperation?.();
                });
            }
        }));
    });
})();
