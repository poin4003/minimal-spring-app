package com.app.features.media.storage.schema;

import java.nio.file.Path;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MediaThumbnailWorkspace {

    private final Path temporaryDirectory;

    private final Path temporaryFile;

    private final Path publishedFile;

    private final String publishedStorageKey;
}
