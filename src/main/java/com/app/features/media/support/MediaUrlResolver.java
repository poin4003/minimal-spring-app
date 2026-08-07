package com.app.features.media.support;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.core.enums.RecordStatus;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.enums.MediaProcessingStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaUrlResolver {

    private final AppProperties appProperties;

    public String resolveContentUrl(MediaEntity media) {
        if (!isDeliveryAvailable(media)) {
            return null;
        }

        return switch (media.getKind()) {
            case VIDEO, AUDIO -> resolveHlsUrl(media.getPublicKey());
            case IMAGE, DOCUMENT, FILE -> resolveOriginalUrl(media.getPublicKey());
        };
    }

    public String resolveOriginalUrl(MediaEntity media) {
        if (!isDeliveryAvailable(media)) {
            return null;
        }

        return resolveOriginalUrl(media.getPublicKey());
    }

    public String resolveThumbnailUrl(MediaEntity media) {
        if (!isDeliveryAvailable(media)
                || media.getThumbnailStorageKey() == null
                || media.getThumbnailStorageKey().isBlank()) {
            return null;
        }

        return UriComponentsBuilder
                .fromPath(appProperties.getMedia().getPublicPath())
                .pathSegment(media.getPublicKey(), "thumbnail")
                .build()
                .encode()
                .toUriString();
    }

    public String resolvePreviewUrl(MediaEntity media) {
        String thumbnailUrl = resolveThumbnailUrl(media);

        return thumbnailUrl != null
                ? thumbnailUrl
                : resolveContentUrl(media);
    }

    private String resolveOriginalUrl(String publicKey) {
        return UriComponentsBuilder
                .fromPath(appProperties.getMedia().getPublicPath())
                .pathSegment(publicKey)
                .build()
                .encode()
                .toUriString();
    }

    private String resolveHlsUrl(String publicKey) {
        return UriComponentsBuilder
                .fromPath(appProperties.getMedia().getPublicPath())
                .pathSegment(publicKey, "hls", "index.m3u8")
                .build()
                .encode()
                .toUriString();
    }

    private boolean isDeliveryAvailable(MediaEntity media) {
        return media != null
                && media.getStatus() == RecordStatus.ACTIVE
                && media.getProcessingStatus() == MediaProcessingStatus.READY;
    }
}
