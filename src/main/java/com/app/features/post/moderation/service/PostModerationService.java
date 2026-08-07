package com.app.features.post.moderation.service;

import java.util.UUID;

import com.app.features.post.moderation.schema.payload.RejectPostPayload;
import com.app.features.post.moderation.schema.result.ModerationStandardPostDetailResult;

import jakarta.validation.Valid;

public interface PostModerationService {

    ModerationStandardPostDetailResult getStandardPostDetail(UUID postId);

    void publishedPost(UUID postId, UUID moderatorId);

    void rejectPost(UUID postId, UUID moderatorId, @Valid RejectPostPayload payload);
}
