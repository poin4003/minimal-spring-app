package com.app.features.post.event;

import java.util.UUID;

public record PostPublishedEvent(UUID postId)
        implements PostMutationEvent {
}
