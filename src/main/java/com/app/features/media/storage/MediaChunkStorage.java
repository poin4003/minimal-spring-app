package com.app.features.media.storage;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import com.app.features.media.storage.schema.StagedMediaFile;

public interface MediaChunkStorage {

    void storeChunk(
            UUID uploadId,
            int chunkIndex,
            long expectedSize,
            String expectedChecksum,
            InputStream inputStream);

    List<Integer> findUploadedChunks(UUID uploadId);

    StagedMediaFile assemble(
            UUID uploadId,
            String originalName,
            String extension,
            long expectedFileSize,
            int totalChunks);

    void deleteUpload(UUID uploadId);
}
