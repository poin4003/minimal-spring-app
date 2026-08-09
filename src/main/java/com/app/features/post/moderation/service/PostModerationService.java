package com.app.features.post.moderation.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.features.post.moderation.schema.filter.ModerationPostFilterCriteria;
import com.app.features.post.moderation.schema.payload.RejectPostPayload;
import com.app.features.post.moderation.schema.result.ModerationPostResult;
import com.app.features.post.moderation.schema.result.ModerationStandardPostDetailResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface PostModerationService {

    Page<ModerationPostResult> getPendingPosts(
            @NotNull ModerationPostFilterCriteria criteria,
            @NotNull Pageable pageable);

    ModerationStandardPostDetailResult getStandardPostDetail(
            @NotNull UUID postId);

    void publishPost(
            @NotNull UUID postId,
            @NotNull UUID moderatorId);

    void rejectPost(
            @NotNull UUID postId,
            @NotNull UUID moderatorId,
            @NotNull @Valid RejectPostPayload payload);
}
