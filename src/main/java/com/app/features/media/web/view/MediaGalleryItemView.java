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

    private static final String FALLBACK_IMAGE_PATH =
            "/vendor/bootstrap-icons/icons/image.svg";
    private static final String FALLBACK_VIDEO_PATH =
            "/vendor/bootstrap-icons/icons/camera-video.svg";
    private static final String FALLBACK_AUDIO_PATH =
            "/vendor/bootstrap-icons/icons/file-music.svg";
    private static final String FALLBACK_DOCUMENT_PATH =
            "/vendor/bootstrap-icons/icons/file-earmark-text.svg";
    private static final String FALLBACK_FILE_PATH =
            "/vendor/bootstrap-icons/icons/file-earmark.svg";

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
    private final String previewPartialPath;
    private final String metadataPath;
    private final String metadataPartialPath;
    private final String detailPath;
    private final String detailPartialPath;
    private final String thumbnailSelectionPath;
    private final String retryPath;
    private final String deletePath;

    public boolean hasThumbnail() {
        return thumbnailUrl != null;
    }

    public String getFallbackImagePath() {
        return switch (kind) {
            case IMAGE -> FALLBACK_IMAGE_PATH;
            case VIDEO -> FALLBACK_VIDEO_PATH;
            case AUDIO -> FALLBACK_AUDIO_PATH;
            case DOCUMENT -> FALLBACK_DOCUMENT_PATH;
            case FILE -> FALLBACK_FILE_PATH;
        };
    }

    public boolean canRetry() {
        return retryPath != null;
    }

    public boolean canSelectThumbnail() {
        return thumbnailSelectionPath != null;
    }
}
