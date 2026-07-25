package com.app.features.media.event;

import java.util.UUID;

public record MediaProcessingFailedEvent(
        UUID mediaId,
        UUID recipientId,
        String originalName) {
}
