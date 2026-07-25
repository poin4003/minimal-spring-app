package com.app.features.media.service;

import java.io.InputStream;
import java.util.UUID;

import com.app.features.media.schema.payload.StartMediaUploadPayload;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.media.schema.result.MediaUploadSessionResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public interface MediaUploadService {

    MediaUploadSessionResult startUpload(
            @NotNull UUID createdById,
            @NotNull @Valid StartMediaUploadPayload payload);

    MediaUploadSessionResult getUpload(
            @NotNull UUID uploadId,
            @NotNull UUID createdById);

    void uploadChunk(
            @NotNull UUID uploadId,
            @NotNull UUID createdById,
            @PositiveOrZero int chunkIndex,
            @Positive long contentLength,
            @NotBlank
            @Pattern(regexp = "^[a-fA-F0-9]{64}$")
            String checksum,
            @NotNull InputStream inputStream);

    MediaResult completeUpload(
            @NotNull UUID uploadId,
            @NotNull UUID createdById);

    void cancelUpload(
            @NotNull UUID uploadId,
            @NotNull UUID createdById);

    int cleanupExpiredUploads();
}
