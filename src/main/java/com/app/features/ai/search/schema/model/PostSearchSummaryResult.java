package com.app.features.ai.search.schema.model;

import java.util.Objects;

import com.app.features.ai.enums.AiAvailability;

public record PostSearchSummaryResult(
        AiAvailability availability,
        PostSearchGeneratedSummary generatedSummary) {

    public PostSearchSummaryResult {
        Objects.requireNonNull(
                availability,
                "availability must not be null");
        if (generatedSummary != null
                && availability != AiAvailability.READY) {
            throw new IllegalArgumentException(
                    "generated summary requires ready generation");
        }
    }

    public static PostSearchSummaryResult unavailable(
            AiAvailability availability) {
        return new PostSearchSummaryResult(availability, null);
    }

    public static PostSearchSummaryResult ready(
            PostSearchGeneratedSummary generatedSummary) {
        return new PostSearchSummaryResult(
                AiAvailability.READY,
                Objects.requireNonNull(
                        generatedSummary,
                        "generatedSummary must not be null"));
    }

    public boolean isGenerated() {
        return generatedSummary != null;
    }
}
