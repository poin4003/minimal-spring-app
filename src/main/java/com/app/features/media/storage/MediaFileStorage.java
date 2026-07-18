package com.app.features.media.storage;

import org.springframework.web.multipart.MultipartFile;

public interface MediaFileStorage {

    StoredMediaFile store(MultipartFile file);

    void delete(String storageKey);
}
