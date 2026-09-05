(function () {
"use strict";

if (window.MediaDirectUploader != null) {
    return;
}

class MediaDirectUploader {
    constructor({
        uploadUrl,
        requestHeadersProvider = null,
        messages
    }) {
        this.uploadUrl = uploadUrl;
        this.requestHeadersProvider = requestHeadersProvider;
        this.messages = messages;
    }

    upload(file, {
        onProgress = null
    } = {}) {
        const xhr = new XMLHttpRequest();
        const formData = new FormData();
        formData.append("file", file, file.name);

        const result = new Promise((resolve, reject) => {
            xhr.open("POST", this.uploadUrl);
            xhr.withCredentials = true;
            const requestHeaders = this.requestHeadersProvider?.() || {};
            Object.entries(requestHeaders).forEach(([name, value]) => {
                if (name && value) {
                    xhr.setRequestHeader(name, value);
                }
            });

            xhr.upload.addEventListener("progress", event => {
                if (event.lengthComputable) {
                    onProgress?.({
                        uploadedBytes: event.loaded,
                        totalBytes: event.total,
                        percent: Math.round((event.loaded / event.total) * 100)
                    });
                }
            });

            xhr.addEventListener("load", () => {
                resolve({
                    ok: xhr.status >= 200 && xhr.status < 300,
                    status: xhr.status,
                    html: xhr.responseText,
                    redirectPath: xhr.getResponseHeader("HX-Redirect")
                });
            });

            xhr.addEventListener("error", () => {
                reject(new Error(this.messages.requestFailed));
            });

            xhr.addEventListener("abort", () => {
                reject(new DOMException(
                    this.messages.cancelled,
                    "AbortError"
                ));
            });

            xhr.send(formData);
        });

        return {
            result,
            cancel: () => xhr.abort()
        };
    }
}

window.MediaDirectUploader = MediaDirectUploader;
})();
