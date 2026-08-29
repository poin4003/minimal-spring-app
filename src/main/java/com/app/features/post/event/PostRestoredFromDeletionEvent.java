package com.app.features.post.event;

import java.util.UUID;

public record PostRestoredFromDeletionEvent(UUID postId)
        implements PostMutationEvent {
}
