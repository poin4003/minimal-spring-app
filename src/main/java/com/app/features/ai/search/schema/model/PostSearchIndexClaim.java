package com.app.features.ai.search.schema.model;

import java.util.Objects;
import java.util.UUID;

public record PostSearchIndexClaim(
        UUID postId,
        long requestedRevision,
        UUID leaseToken) {

    public PostSearchIndexClaim {
        Objects.requireNonNull(postId, "postId must not be null");
        Objects.requireNonNull(leaseToken, "leaseToken must not be null");
    }
}
