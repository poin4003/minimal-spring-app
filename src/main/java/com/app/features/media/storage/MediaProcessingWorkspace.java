package com.app.features.media.storage;

import java.nio.file.Path;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MediaProcessingWorkspace {

    private final Path temporaryDirectory;

    private final Path publishedDirectory;

    private final String publishedDirectoryKey;
}
