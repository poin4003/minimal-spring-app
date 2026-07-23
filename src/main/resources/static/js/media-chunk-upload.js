export class MediaChunkUploadError extends Error {
    constructor(message, {
        status = 0,
        error = null
    } = {}) {
        super(message);
        this.name = "MediaChunkUploadError";
        this.status = status;
        this.error = error;
    }
}

export class MediaChunkUploader {
    constructor({
        baseUrl = "/api/v1/media/uploads",
        concurrency = 3,
        accessTokenProvider = null,
        requestHeadersProvider = null
    } = {}) {
        this.baseUrl = baseUrl;
        this.concurrency = Math.max(1, concurrency);
        this.accessTokenProvider = accessTokenProvider;
        this.requestHeadersProvider = requestHeadersProvider;
    }

    async upload(file, {
        uploadId = null,
        onSession = null,
        onProgress = null,
        signal = null
    } = {}) {
        const session = await this.resolveSession(file, uploadId, signal);
        onSession?.(session);

        if (session.status === "COMPLETED") {
            this.reportProgress(onProgress, file.size, file.size);
            return session.completedMedia;
        }
        if (session.status !== "UPLOADING") {
            throw new MediaChunkUploadError(
                "The upload session cannot be resumed in its current state.",
                { error: "UPLOAD_SESSION_NOT_RESUMABLE" }
            );
        }
        if (session.originalName !== file.name || session.fileSize !== file.size) {
            throw new MediaChunkUploadError(
                "The selected file does not match the saved upload session.",
                { error: "UPLOAD_FILE_MISMATCH" }
            );
        }

        const uploaded = new Set(session.uploadedChunks);
        const pending = Array.from(
            { length: session.totalChunks },
            (_, index) => index
        ).filter(index => !uploaded.has(index));
        let cursor = 0;
        let uploadedBytes = [...uploaded].reduce(
            (total, index) => total + this.chunkSize(file, session, index),
            0
        );

        this.reportProgress(onProgress, uploadedBytes, file.size);

        const worker = async () => {
            while (cursor < pending.length) {
                const index = pending[cursor++];
                const start = index * session.chunkSize;
                const chunk = file.slice(
                    start,
                    Math.min(start + session.chunkSize, file.size)
                );
                const checksum = await this.sha256(chunk);

                await this.request(
                    `${this.baseUrl}/${session.id}/chunks/${index}`,
                    {
                        method: "PUT",
                        headers: {
                            "Content-Type": "application/octet-stream",
                            "X-Chunk-SHA256": checksum
                        },
                        body: chunk,
                        signal
                    }
                );

                uploadedBytes += chunk.size;
                this.reportProgress(onProgress, uploadedBytes, file.size);
            }
        };

        const workerCount = Math.min(this.concurrency, pending.length);
        await Promise.all(Array.from(
            { length: workerCount },
            () => worker()
        ));

        return this.request(`${this.baseUrl}/${session.id}/complete`, {
            method: "POST",
            signal
        });
    }

    async resolveSession(file, uploadId, signal) {
        if (uploadId == null) {
            return this.startUpload(file, signal);
        }

        try {
            return await this.getUpload(uploadId, signal);
        } catch (error) {
            if (!(error instanceof MediaChunkUploadError)
                    || error.error !== "RESOURCE_NOT_FOUND"
                    && error.error !== "INVALID_PARAM") {
                throw error;
            }

            return this.startUpload(file, signal);
        }
    }

    startUpload(file, signal = null) {
        return this.request(this.baseUrl, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                originalName: file.name,
                fileSize: file.size
            }),
            signal
        });
    }

    getUpload(uploadId, signal = null) {
        return this.request(`${this.baseUrl}/${uploadId}`, { signal });
    }

    cancel(uploadId, signal = null) {
        return this.request(`${this.baseUrl}/${uploadId}`, {
            method: "DELETE",
            signal
        });
    }

    chunkSize(file, session, index) {
        const start = index * session.chunkSize;
        return Math.min(session.chunkSize, file.size - start);
    }

    reportProgress(callback, uploadedBytes, totalBytes) {
        callback?.({
            uploadedBytes,
            totalBytes,
            percent: totalBytes === 0
                ? 0
                : Math.round((uploadedBytes / totalBytes) * 100)
        });
    }

    async sha256(blob) {
        const bytes = await blob.arrayBuffer();
        const digest = await crypto.subtle.digest("SHA-256", bytes);
        return [...new Uint8Array(digest)]
            .map(value => value.toString(16).padStart(2, "0"))
            .join("");
    }

    async request(url, options = {}) {
        const token = this.accessTokenProvider
            ? await this.accessTokenProvider()
            : null;
        const requestHeaders = this.requestHeadersProvider
            ? await this.requestHeadersProvider()
            : {};
        const response = await fetch(url, {
            credentials: "include",
            ...options,
            headers: {
                ...requestHeaders,
                ...(options.headers || {}),
                ...(token ? { Authorization: `Bearer ${token}` } : {})
            }
        });

        const redirectPath = response.headers.get("HX-Redirect");
        if (redirectPath) {
            window.location.assign(redirectPath);
            throw new MediaChunkUploadError("Authentication is required.", {
                status: response.status,
                error: "AUTHENTICATION_REQUIRED"
            });
        }

        const contentType = response.headers.get("Content-Type") || "";
        const payload = contentType.includes("application/json")
            ? await response.json()
            : null;

        if (!response.ok || payload?.success !== true) {
            throw new MediaChunkUploadError(
                payload?.message || "Media upload request failed.",
                {
                    status: response.status,
                    error: payload?.error || null
                }
            );
        }
        return payload.result;
    }
}
