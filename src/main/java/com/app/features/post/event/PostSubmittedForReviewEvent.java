package com.app.features.post.event;

import java.util.UUID;

public record PostSubmittedForReviewEvent(UUID postId) {
}
