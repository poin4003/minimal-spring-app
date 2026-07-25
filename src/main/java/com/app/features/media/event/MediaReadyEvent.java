package com.app.features.media.event;

import java.util.UUID;

public record MediaReadyEvent(
        UUID mediaId,
        UUID recipientId,
        String originalName) {
}
