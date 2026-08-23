package com.app.features.post.aimoderation.service;

import com.app.features.post.aimoderation.entity.PostAiModerationConfigEntity;

public interface PostAiModerationConfigService {

    PostAiModerationConfigEntity requireCurrentConfig();

    PostAiModerationConfigEntity requireCurrentConfigForUpdate();
}
