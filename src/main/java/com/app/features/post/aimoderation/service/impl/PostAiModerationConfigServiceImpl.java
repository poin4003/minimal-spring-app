package com.app.features.post.aimoderation.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.core.exception.ExceptionFactory;
import com.app.features.post.aimoderation.entity.PostAiModerationConfigEntity;
import com.app.features.post.aimoderation.repository.PostAiModerationConfigRepository;
import com.app.features.post.aimoderation.service.PostAiModerationConfigService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostAiModerationConfigServiceImpl implements PostAiModerationConfigService {

    private static final String DEFAULT_CONFIG_CODE = "DEFAULT";

    private final PostAiModerationConfigRepository postAiModerationConfigRepo;

    @Override
    public PostAiModerationConfigEntity requireCurrentConfig() {
        return postAiModerationConfigRepo.findByCode(DEFAULT_CONFIG_CODE)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.post.aiModerationConfigNotFound"));
    }
}
