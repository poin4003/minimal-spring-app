package com.app.features.ai.search.schema.model;

import java.util.List;
import java.util.Objects;

import com.app.features.ai.enums.AiAvailability;

public record PostSemanticSearchResult(
        AiAvailability availability,
        List<PostVectorSearchHit> hits) {

    public PostSemanticSearchResult {
        Objects.requireNonNull(
                availability,
                "availability must not be null");
        hits = List.copyOf(Objects.requireNonNull(
                hits,
                "hits must not be null"));
    }

    public static PostSemanticSearchResult unavailable(
            AiAvailability availability) {
        return new PostSemanticSearchResult(availability, List.of());
    }

    public static PostSemanticSearchResult ready(
            List<PostVectorSearchHit> hits) {
        return new PostSemanticSearchResult(
                AiAvailability.READY,
                hits);
    }
}
