package com.app.features.post.aimoderation.service;

import com.app.features.post.aimoderation.schema.model.PostAiModerationCandidate;
import com.app.features.post.aimoderation.schema.model.PostAiModerationClientResult;

import jakarta.validation.constraints.NotNull;

public interface PostAiModerationDecisionService {

    void apply(
            @NotNull PostAiModerationCandidate candidate,
            @NotNull PostAiModerationClientResult result);

    void recordError(
            @NotNull PostAiModerationCandidate candidate,
            @NotNull RuntimeException exception);
}
