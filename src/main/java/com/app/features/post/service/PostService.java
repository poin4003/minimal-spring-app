package com.app.features.post.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.app.features.post.entity.PostEntity;
import com.app.features.post.enums.PostType;
import com.app.features.user.entity.UserBaseEntity;

import jakarta.validation.constraints.NotNull;

public interface PostService {

    PostEntity createDraftPost(
            @NotNull UserBaseEntity author,
            @NotNull PostType type);

    PostEntity requirePost(@NotNull UUID postId);

    PostEntity requireOwnedPost(
            @NotNull PostEntity post,
            @NotNull UUID ownerId);

    PostEntity prepareOwnedPostForUpdate(
            @NotNull UUID postId,
            @NotNull UUID ownerId);

    PostEntity requireOwnedPostForUpdate(
            @NotNull UUID postId,
            @NotNull UUID ownerId);

    void submitForReview(@NotNull PostEntity post);

    PostEntity requirePendingPost(@NotNull PostEntity post);

    PostEntity requirePendingPostForUpdate(@NotNull UUID postId);

    Optional<PostEntity> findPendingPost(@NotNull UUID postId);

    Optional<PostEntity> findPendingPostForUpdate(
            @NotNull UUID postId,
            @NotNull LocalDateTime expectedUpdatedAt);

    void deletePost(@NotNull PostEntity post);
}
