package com.app.features.media.service;

import java.io.InputStream;
import java.util.UUID;

import com.app.features.media.schema.payload.StartMediaUploadPayload;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.media.schema.result.MediaUploadSessionResult;

public interface MediaUploadService {

    MediaUploadSessionResult startUpload(
            UUID createdById,
            StartMediaUploadPayload payload);

    MediaUploadSessionResult getUpload(UUID uploadId, UUID createdById);

    void uploadChunk(
            UUID uploadId,
            UUID createdById,
            int chunkIndex,
            long contentLength,
            String checksum,
            InputStream inputStream);

    MediaResult completeUpload(UUID uploadId, UUID createdById);

    void cancelUpload(UUID uploadId, UUID createdById);

    int cleanupExpiredUploads();
}
