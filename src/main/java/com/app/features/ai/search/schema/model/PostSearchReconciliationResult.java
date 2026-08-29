package com.app.features.ai.search.schema.model;

import java.util.Objects;

import com.app.features.ai.enums.AiAvailability;

public record PostSearchReconciliationResult(
        AiAvailability availability,
        int recoveryCandidates,
        int backfillStatesCreated,
        int jobsEnqueued,
        int failures) {

    public PostSearchReconciliationResult {
        Objects.requireNonNull(
                availability,
                "availability must not be null");
    }

    public static PostSearchReconciliationResult skipped(
            AiAvailability availability) {
        return new PostSearchReconciliationResult(
                availability,
                0,
                0,
                0,
                0);
    }
}
