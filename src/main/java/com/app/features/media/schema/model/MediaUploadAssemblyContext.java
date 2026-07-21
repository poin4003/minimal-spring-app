package com.app.features.media.schema.model;

import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MediaUploadAssemblyContext {

    private final UUID uploadId;

    private final String originalName;

    private final String extension;

    private final long fileSize;

    private final int totalChunks;
}
