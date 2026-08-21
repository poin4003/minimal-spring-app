package com.app.features.post.aimoderation.service;

import com.app.features.post.aimoderation.schema.model.PostAiModerationClientResult;
import com.app.features.post.aimoderation.schema.model.PostAiModerationRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface PostAiModerationClient {

    PostAiModerationClientResult moderate(@NotNull @Valid PostAiModerationRequest request);
}
