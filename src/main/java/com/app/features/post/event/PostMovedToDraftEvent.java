package com.app.features.post.event;

import java.util.UUID;

public record PostMovedToDraftEvent(UUID postId)
        implements PostMutationEvent {
}
