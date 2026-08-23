package com.app.features.post.aimoderation.service;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public interface PostAiModerationWorkflowService {

    void moderate(@NotNull UUID postId);
}
