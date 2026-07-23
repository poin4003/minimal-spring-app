package com.app.features.media.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaUploadTransportView {

    private final String directUploadPath;
    private final String chunkUploadPath;
    private final long directUploadThresholdBytes;
    private final int parallelChunks;
}
