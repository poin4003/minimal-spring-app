package com.app.features.media.storage.schema;

import java.nio.file.Path;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StagedMediaFile {

    private final Path temporaryPath;

    private final String originalName;

    private final String extension;

    private final long fileSize;
}
