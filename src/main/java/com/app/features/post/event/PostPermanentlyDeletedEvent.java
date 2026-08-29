package com.app.features.post.event;

import java.util.UUID;

public record PostPermanentlyDeletedEvent(UUID postId)
        implements PostMutationEvent {
}
