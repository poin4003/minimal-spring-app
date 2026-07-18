package com.app.features.media.storage;

import lombok.Data;

@Data
public class StoredMediaFile {

    private String storageKey;

    private String originalName;

    private String contentType;

    private long fileSize;
}
