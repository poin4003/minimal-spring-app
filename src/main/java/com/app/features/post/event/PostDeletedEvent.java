package com.app.features.post.event;

import java.util.UUID;

public record PostDeletedEvent(UUID postId)
        implements PostMutationEvent {
}
