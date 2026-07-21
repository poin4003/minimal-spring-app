package com.app.features.media.storage.schema;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StoredMediaFile {

    private final String storageKey;

    private final String originalName;

    private final String contentType;

    private final long fileSize;
}
