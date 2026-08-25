package com.app.features.post.aimoderation.service.impl;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.app.features.post.aimoderation.entity.PostAiModerationDecisionLogEntity;
import com.app.features.post.aimoderation.entity.PostAiModerationConfigEntity;
import com.app.features.post.aimoderation.enums.PostAiModerationMode;
import com.app.features.post.aimoderation.enums.PostAiModerationOutcome;
import com.app.features.post.aimoderation.exceptions.PostAiModerationClientException;
import com.app.features.post.aimoderation.repository.PostAiModerationDecisionLogRepository;
import com.app.features.post.aimoderation.schema.model.PostAiModerationCandidate;
import com.app.features.post.aimoderation.schema.model.PostAiModerationClientResult;
import com.app.features.post.aimoderation.service.PostAiModerationConfigService;
import com.app.features.post.aimoderation.service.PostAiModerationDecisionService;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.moderation.service.PostModerationCommandService;
import com.app.features.post.service.PostService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class PostAiModerationDecisionServiceImpl
        implements PostAiModerationDecisionService {

    private static final int MAX_REJECTION_REASON_LENGTH = 1_000;
    private static final String DEFAULT_REJECTION_REASON =
            "Rejected by AI moderation.";
    private static final String STALE_DECISION_REASON =
            "Post changed while AI moderation was running.";
    private static final String STALE_POLICY_REASON =
            "Moderation policy changed while AI was running.";

    private final PostAiModerationConfigService postAiModerationConfigSvc;
    private final PostModerationCommandService postModerationCommandSvc;
    private final PostService postSvc;
    private final PostAiModerationDecisionLogRepository
            postAiModerationDecisionLogRepo;

    @Override
    public void apply(
            PostAiModerationCandidate candidate,
            PostAiModerationClientResult result) {
        PostAiModerationConfigEntity config = postAiModerationConfigSvc
                .requireCurrentConfigForUpdate();

        if (config.getMode() != PostAiModerationMode.AUTO) {
            return;
        }

        if (!Objects.equals(
                config.getUpdatedAt(),
                candidate.configUpdatedAt())) {
            saveLog(
                    postSvc.requirePost(candidate.postId()),
                    candidate.promptSnapshot(),
                    PostAiModerationOutcome.ESCALATE,
                    STALE_POLICY_REASON,
                    result.rawResponse(),
                    null,
                    result.modelName());
            return;
        }

        Optional<PostEntity> moderatedPost = switch (result.outcome()) {
            case APPROVE -> postModerationCommandSvc
                    .publishPostAutomatically(
                            candidate.postId(),
                            candidate.postUpdatedAt());
            case REJECT -> postModerationCommandSvc
                    .rejectPostAutomatically(
                            candidate.postId(),
                            candidate.postUpdatedAt(),
                            limitRejectionReason(result.reason()));
            case ESCALATE, ERROR -> postSvc.findPendingPostForUpdate(
                    candidate.postId(),
                    candidate.postUpdatedAt());
        };

        if (moderatedPost.isEmpty()) {
            saveLog(
                    postSvc.requirePost(candidate.postId()),
                    candidate.promptSnapshot(),
                    PostAiModerationOutcome.ESCALATE,
                    STALE_DECISION_REASON,
                    result.rawResponse(),
                    null,
                    result.modelName());
            return;
        }

        saveLog(
                moderatedPost.get(),
                candidate.promptSnapshot(),
                result.outcome(),
                result.reason(),
                result.rawResponse(),
                null,
                result.modelName());
    }

    @Override
    public void recordError(
            PostAiModerationCandidate candidate,
            RuntimeException exception) {
        String errorMessage = StringUtils.hasText(exception.getMessage())
                ? exception.getMessage()
                : exception.getClass().getSimpleName();
        String rawResponse = exception instanceof PostAiModerationClientException
                ? ((PostAiModerationClientException) exception).getRawResponse()
                : null;

        saveLog(
                postSvc.requirePost(candidate.postId()),
                candidate.promptSnapshot(),
                PostAiModerationOutcome.ERROR,
                null,
                rawResponse,
                errorMessage,
                null);
    }

    private String limitRejectionReason(String reason) {
        String normalizedReason = StringUtils.hasText(reason)
                ? reason.trim()
                : DEFAULT_REJECTION_REASON;

        return normalizedReason.substring(
                0,
                Math.min(
                        normalizedReason.length(),
                        MAX_REJECTION_REASON_LENGTH));
    }

    private void saveLog(
            PostEntity post,
            String promptSnapshot,
            PostAiModerationOutcome outcome,
            String reason,
            String rawResponse,
            String errorMessage,
            String modelName) {
        PostAiModerationDecisionLogEntity log =
                new PostAiModerationDecisionLogEntity();
        log.setPost(post);
        log.setOutcome(outcome);
        log.setPromptSnapshot(promptSnapshot);
        log.setReason(reason);
        log.setRawResponse(rawResponse);
        log.setErrorMessage(errorMessage);
        log.setModelName(modelName);

        postAiModerationDecisionLogRepo.save(log);
    }
}
