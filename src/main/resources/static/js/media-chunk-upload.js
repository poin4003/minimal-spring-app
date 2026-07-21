export class MediaChunkUploader {
    constructor({
        baseUrl = "/api/v1/media/uploads",
        concurrency = 3,
        accessTokenProvider = null
    } = {}) {
        this.baseUrl = baseUrl;
        this.concurrency = Math.max(1, concurrency);
        this.accessTokenProvider = accessTokenProvider;
    }

    async upload(file, {
        uploadId = null,
        onProgress = null,
        signal = null
    } = {}) {
        const session = uploadId
            ? await this.getUpload(uploadId, signal)
            : await this.startUpload(file, signal);

        if (session.status === "COMPLETED") {
            return session.completedMedia;
        }
        if (session.originalName !== file.name || session.fileSize !== file.size) {
            throw new Error("The selected file does not match the upload session.");
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
                onProgress?.({
                    uploadedBytes,
                    totalBytes: file.size,
                    percent: Math.round((uploadedBytes / file.size) * 100)
                });
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
        const response = await fetch(url, {
            credentials: "include",
            ...options,
            headers: {
                ...(options.headers || {}),
                ...(token ? { Authorization: `Bearer ${token}` } : {})
            }
        });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            throw new Error(payload.message || "Media upload request failed.");
        }
        return payload.result;
    }
}
