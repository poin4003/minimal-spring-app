package com.app.features.ai.search.schema.model;

import java.util.List;
import java.util.Objects;

import com.app.features.ai.enums.AiAvailability;

public record PostSearchResult(
        String query,
        AiAvailability availability,
        List<PostSearchItem> items) {

    public PostSearchResult {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        query = query.trim();
        Objects.requireNonNull(
                availability,
                "availability must not be null");
        items = List.copyOf(Objects.requireNonNull(
                items,
                "items must not be null"));
    }

    public static PostSearchResult unavailable(
            String query,
            AiAvailability availability) {
        return new PostSearchResult(query, availability, List.of());
    }

    public static PostSearchResult ready(
            String query,
            List<PostSearchItem> items) {
        return new PostSearchResult(
                query,
                AiAvailability.READY,
                items);
    }
}
