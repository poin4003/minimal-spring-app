package com.app.features.post.aimoderation.service.impl;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.app.core.constant.PermissionConstants;
import com.app.core.exception.ExceptionFactory;
import com.app.features.post.aimoderation.entity.PostAiModerationConfigEntity;
import com.app.features.post.aimoderation.entity.PostAiModerationDecisionLogEntity;
import com.app.features.post.aimoderation.enums.PostAiModerationMode;
import com.app.features.post.aimoderation.repository.PostAiModerationDecisionLogRepository;
import com.app.features.post.aimoderation.schema.result.PostAiModerationConfigResult;
import com.app.features.post.aimoderation.schema.result.PostAiModerationDecisionLogDetailResult;
import com.app.features.post.aimoderation.schema.result.PostAiModerationDecisionLogResult;
import com.app.features.post.aimoderation.service.PostAiModerationAdminService;
import com.app.features.post.aimoderation.service.PostAiModerationConfigService;
import com.app.features.post.service.PostService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@Secured(PermissionConstants.POST_MODERATE)
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostAiModerationAdminServiceImpl
        implements PostAiModerationAdminService {

    private final PostAiModerationConfigService postAiModerationConfigSvc;
    private final PostAiModerationDecisionLogRepository
            postAiModerationDecisionLogRepo;
    private final PostService postSvc;
    private final ModelMapper mapper;

    @Override
    public PostAiModerationConfigResult getConfig() {
        return mapper.map(
                postAiModerationConfigSvc.requireCurrentConfig(),
                PostAiModerationConfigResult.class);
    }

    @Override
    @Transactional
    public void updateConfig(
            PostAiModerationMode mode,
            String promptText) {
        if (mode == PostAiModerationMode.AUTO
                && !StringUtils.hasText(promptText)) {
            throw ExceptionFactory.invalidParam(
                    "error.post.aiModerationPromptRequired");
        }

        PostAiModerationConfigEntity config =
                postAiModerationConfigSvc
                        .requireCurrentConfigForUpdate();

        config.setMode(mode);
        config.setPromptText(StringUtils.hasText(promptText)
                ? promptText.trim()
                : null);
    }

    @Override
    public Page<PostAiModerationDecisionLogResult> getDecisionLogs(
            UUID postId,
            Pageable pageable) {
        postSvc.requirePost(postId);
        return postAiModerationDecisionLogRepo
                .findAllByPost_Id(postId, pageable)
                .map(log -> toResult(log));
    }

    @Override
    public PostAiModerationDecisionLogDetailResult getDecisionLogDetail(
            UUID postId,
            UUID logId) {
        postSvc.requirePost(postId);
        PostAiModerationDecisionLogEntity log =
                postAiModerationDecisionLogRepo
                        .findByIdAndPost_Id(logId, postId)
                        .orElseThrow(() -> ExceptionFactory.notFound(
                                "error.post.aiModerationLogNotFound",
                                logId));

        PostAiModerationDecisionLogDetailResult result = mapper.map(
                log,
                PostAiModerationDecisionLogDetailResult.class);
        result.setPostId(log.getPost().getId());

        return result;
    }

    private PostAiModerationDecisionLogResult toResult(
            PostAiModerationDecisionLogEntity log) {
        PostAiModerationDecisionLogResult result = mapper.map(
                log,
                PostAiModerationDecisionLogResult.class);
        result.setPostId(log.getPost().getId());

        return result;
    }
}
