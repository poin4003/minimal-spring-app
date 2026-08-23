package com.app.features.post.moderation.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.features.post.moderation.schema.filter.ModerationPostFilterCriteria;
import com.app.features.post.moderation.schema.payload.RejectPostPayload;
import com.app.features.post.moderation.schema.result.ModerationPostResult;
import com.app.features.post.moderation.schema.result.ModerationPostDetailResult;
import com.app.features.post.moderation.schema.result.ModerationShortPostDetailResult;
import com.app.features.post.moderation.schema.result.ModerationStandardPostDetailResult;
import com.app.features.post.moderation.schema.result.ModerationVideoPostDetailResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface PostModerationService {

    Page<ModerationPostResult> getPosts(
            @NotNull ModerationPostFilterCriteria criteria,
            @NotNull Pageable pageable);

    ModerationPostDetailResult getPostDetail(@NotNull UUID postId);

    ModerationStandardPostDetailResult getStandardPostDetail(
            @NotNull UUID postId);

    ModerationShortPostDetailResult getShortPostDetail(
            @NotNull UUID postId);

    ModerationVideoPostDetailResult getVideoPostDetail(
            @NotNull UUID postId);

    void publishPost(
            @NotNull UUID postId,
            @NotNull UUID moderatorId);

    void rejectPost(
            @NotNull UUID postId,
            @NotNull UUID moderatorId,
            @NotNull @Valid RejectPostPayload payload);
}
