package com.app.features.post.videopost.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.videopost.entity.VideoPostEntity;
import com.app.features.post.videopost.schema.filter.OwnerVideoPostFilterCriteria;
import com.app.features.post.videopost.schema.filter.PublicVideoPostFilterCriteria;
import com.app.features.post.videopost.schema.payload.CreateVideoPostPayload;
import com.app.features.post.videopost.schema.payload.UpdateVideoPostPayload;
import com.app.features.post.videopost.schema.result.OwnerVideoPostResult;
import com.app.features.post.videopost.schema.result.PublicVideoPostResult;
import com.app.features.user.entity.UserBaseEntity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface VideoPostService {

    OwnerVideoPostResult createVideoPost(
            @NotNull UUID authorId,
            @NotNull @Valid CreateVideoPostPayload payload);

    VideoPostEntity createDraftVideoPost(
            @NotNull UserBaseEntity author,
            @NotNull @Valid CreateVideoPostPayload payload);

    OwnerVideoPostResult updateOwnedVideoPost(
            @NotNull UUID postId,
            @NotNull UUID ownerId,
            @NotNull @Valid UpdateVideoPostPayload payload);

    void submitOwnedPostForReview(
            @NotNull UUID postId,
            @NotNull UUID ownerId);

    PublicVideoPostResult getPublishedPost(@NotNull UUID postId);

    OwnerVideoPostResult getOwnerPost(
            @NotNull UUID postId,
            @NotNull UUID ownerId);

    Page<PublicVideoPostResult> getPublishedPosts(
            @NotNull PublicVideoPostFilterCriteria criteria,
            @NotNull Pageable pageable);

    Page<OwnerVideoPostResult> getOwnedPosts(
            @NotNull UUID ownerId,
            @NotNull OwnerVideoPostFilterCriteria criteria,
            @NotNull Pageable pageable);

    VideoPostEntity requireVideoPost(@NotNull UUID postId);

    List<VideoPostEntity> requireOwnedVideoPosts(
            @NotNull Collection<@NotNull UUID> postIds,
            @NotNull UUID ownerId);

    PostMediaEntity requireContentAttachment(@NotNull UUID postId);

    Map<UUID, PostMediaEntity> requireContentAttachments(
            @NotNull Collection<@NotNull UUID> postIds);
}
