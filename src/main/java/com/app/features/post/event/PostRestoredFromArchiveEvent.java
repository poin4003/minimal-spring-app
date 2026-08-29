package com.app.features.post.event;

import java.util.UUID;

public record PostRestoredFromArchiveEvent(UUID postId)
        implements PostMutationEvent {
}
