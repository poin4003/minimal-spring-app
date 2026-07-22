package com.app.features.media.web.view;

import java.util.UUID;

import com.app.core.enums.RecordStatus;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaGalleryItemView {

    private final UUID id;
    private final String originalName;
    private final String thumbnailUrl;
    private final MediaKind kind;
    private final MediaProcessingStatus processingStatus;
    private final RecordStatus status;
    private final String createdByEmail;
    private final String fileSizeLabel;
    private final String createdAt;
    private final String previewPath;
    private final String metadataPath;
    private final String detailPath;
    private final String retryPath;
    private final String deletePath;

    public boolean hasThumbnail() {
        return thumbnailUrl != null;
    }

    public boolean canRetry() {
        return retryPath != null;
    }
}
