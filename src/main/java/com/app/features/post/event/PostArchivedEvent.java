package com.app.features.post.event;

import java.util.UUID;

public record PostArchivedEvent(UUID postId)
        implements PostMutationEvent {
}
