package com.app.features.post.aimoderation.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.app.features.post.aimoderation.entity.PostAiModerationConfigEntity;
import com.app.features.post.aimoderation.enums.PostAiModerationMode;
import com.app.features.post.aimoderation.schema.model.PostAiModerationCandidate;
import com.app.features.post.aimoderation.schema.model.PostAiModerationClientResult;
import com.app.features.post.aimoderation.service.PostAiModerationClient;
import com.app.features.post.aimoderation.service.PostAiModerationConfigService;
import com.app.features.post.aimoderation.service.PostAiModerationDecisionService;
import com.app.features.post.aimoderation.service.PostAiModerationWorkflowService;
import com.app.features.post.aimoderation.support.PostAiModerationRequestFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class PostAiModerationWorkflowServiceImpl
        implements PostAiModerationWorkflowService {

    private final PostAiModerationConfigService postAiModerationConfigSvc;
    private final PostAiModerationRequestFactory postAiModerationRequestFactory;
    private final PostAiModerationClient postAiModerationClient;
    private final PostAiModerationDecisionService postAiModerationDecisionSvc;

    @Override
    public void moderate(UUID postId) {
        PostAiModerationCandidate candidate = null;

        try {
            PostAiModerationConfigEntity config =
                    postAiModerationConfigSvc.requireCurrentConfig();

            if (config.getMode() != PostAiModerationMode.AUTO) {
                return;
            }

            if (!StringUtils.hasText(config.getPromptText())) {
                log.warn(
                        "AI moderation prompt is blank; post [{}] remains pending.",
                        postId);
                return;
            }

            candidate = postAiModerationRequestFactory
                    .create(
                            postId,
                            config.getPromptText(),
                            config.getUpdatedAt())
                    .orElse(null);

            if (candidate == null) {
                return;
            }

            PostAiModerationClientResult result =
                    postAiModerationClient.moderate(candidate.request());
            postAiModerationDecisionSvc.apply(candidate, result);
        } catch (RuntimeException exception) {
            if (candidate != null) {
                recordErrorSafely(candidate, exception);
            }

            log.error(
                    "AI moderation failed for post [{}]; post remains pending.",
                    postId,
                    exception);
        }
    }

    private void recordErrorSafely(
            PostAiModerationCandidate candidate,
            RuntimeException exception) {
        try {
            postAiModerationDecisionSvc.recordError(candidate, exception);
        } catch (RuntimeException logException) {
            log.error(
                    "Unable to record AI moderation error for post [{}].",
                    candidate.postId(),
                    logException);
        }
    }
}
