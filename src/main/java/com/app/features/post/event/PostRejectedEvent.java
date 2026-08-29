package com.app.features.post.event;

import java.util.UUID;

public record PostRejectedEvent(UUID postId)
        implements PostMutationEvent {
}
