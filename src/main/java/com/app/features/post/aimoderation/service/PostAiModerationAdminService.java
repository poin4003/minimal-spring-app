package com.app.features.post.aimoderation.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.features.post.aimoderation.enums.PostAiModerationMode;
import com.app.features.post.aimoderation.schema.result.PostAiModerationConfigResult;
import com.app.features.post.aimoderation.schema.result.PostAiModerationDecisionLogDetailResult;
import com.app.features.post.aimoderation.schema.result.PostAiModerationDecisionLogResult;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public interface PostAiModerationAdminService {

    PostAiModerationConfigResult getConfig();

    void updateConfig(
            @NotNull PostAiModerationMode mode,
            @Size(max = 10_000) String promptText);

    Page<PostAiModerationDecisionLogResult> getDecisionLogs(
            @NotNull UUID postId,
            @NotNull Pageable pageable);

    PostAiModerationDecisionLogDetailResult getDecisionLogDetail(
            @NotNull UUID postId,
            @NotNull UUID logId);
}
