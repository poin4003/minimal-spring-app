package com.app.features.media.schema.result;

import java.nio.file.Path;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MediaDeliveryResult {

    private final Path path;
    private final String contentType;
    private final String fileName;
    private final boolean attachment;
}
