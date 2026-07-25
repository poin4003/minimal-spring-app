package com.app.features.media.event;

import java.util.UUID;

import com.app.features.media.enums.MediaProcessingStatus;

public record MediaUploadedEvent(
        UUID mediaId,
        UUID recipientId,
        String originalName,
        MediaProcessingStatus processingStatus) {
}
