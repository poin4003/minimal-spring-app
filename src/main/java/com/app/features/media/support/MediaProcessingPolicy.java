package com.app.features.media.support;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.enums.MediaKind;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaProcessingPolicy {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final AppProperties appProperties;

    public boolean requiresProcessing(MediaKind kind, String contentType) {
        return requiresHls(kind) || shouldGenerateThumbnail(kind, contentType);
    }

    public boolean shouldGenerateThumbnail(MediaEntity media) {
        return shouldGenerateThumbnail(media.getKind(), media.getContentType());
    }

    public boolean shouldGenerateThumbnail(MediaKind kind, String contentType) {
        AppProperties.Thumbnail config = appProperties.getMedia().getThumbnail();
        if (!config.isEnabled()) {
            return false;
        }

        return switch (kind) {
            case IMAGE, VIDEO -> true;
            case AUDIO -> config.isAudioCoverEnabled();
            case DOCUMENT -> config.isPdfEnabled()
                    && PDF_CONTENT_TYPE.equalsIgnoreCase(contentType);
            case FILE -> false;
        };
    }

    public boolean isThumbnailRequired(MediaKind kind) {
        return kind == MediaKind.IMAGE || kind == MediaKind.VIDEO;
    }

    public boolean requiresHls(MediaKind kind) {
        return kind == MediaKind.VIDEO || kind == MediaKind.AUDIO;
    }

    public boolean supportsManualThumbnail(MediaKind kind) {
        return kind == MediaKind.VIDEO || kind == MediaKind.AUDIO;
    }
}
