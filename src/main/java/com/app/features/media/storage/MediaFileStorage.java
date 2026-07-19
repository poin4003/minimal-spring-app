package com.app.features.media.storage;

import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

import com.app.config.settings.AppProperties.AllowedMediaType;

public interface MediaFileStorage {

    StagedMediaFile stage(MultipartFile file, AllowedMediaType policy);

    StoredMediaFile commit(
            StagedMediaFile stagedFile,
            String detectedContentType);

    void discard(StagedMediaFile stagedFile);

    MediaProcessingWorkspace prepareProcessingWorkspace(String sourceStorageKey);

    void publishProcessingWorkspace(MediaProcessingWorkspace workspace);

    void discardProcessingWorkspace(MediaProcessingWorkspace workspace);

    Path resolve(String storageKey);

    boolean exists(String storageKey);

    boolean deleteHlsArtifacts(String sourceStorageKey);

    void delete(String storageKey);
}
