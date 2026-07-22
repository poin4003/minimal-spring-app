package com.app.features.media.schema.result;

import java.nio.file.Path;

import lombok.Getter;

@Getter
public class MediaDeliveryResult {

    private final Path path;
    private final String contentType;
    private final String fileName;
    private final boolean attachment;
    private final boolean cacheImmutable;

    public MediaDeliveryResult(
            Path path,
            String contentType,
            String fileName,
            boolean attachment) {
        this(path, contentType, fileName, attachment, true);
    }

    public MediaDeliveryResult(
            Path path,
            String contentType,
            String fileName,
            boolean attachment,
            boolean cacheImmutable) {
        this.path = path;
        this.contentType = contentType;
        this.fileName = fileName;
        this.attachment = attachment;
        this.cacheImmutable = cacheImmutable;
    }
}
