package com.app.features.post.service;

import java.util.UUID;

import com.app.features.post.entity.PostEntity;
import com.app.features.post.enums.PostType;
import com.app.features.user.entity.UserBaseEntity;

import jakarta.validation.constraints.NotNull;

public interface PostService {

    PostEntity createDraftPost(
            @NotNull UserBaseEntity author,
            @NotNull PostType type);

    PostEntity requireOwnedPost(
            @NotNull PostEntity post,
            @NotNull UUID ownerId);

    PostEntity prepareOwnedPostForUpdate(
            @NotNull UUID postId,
            @NotNull UUID ownerId);

    PostEntity requireOwnedPostForUpdate(
            @NotNull UUID postId,
            @NotNull UUID ownerId);

    PostEntity submitForReview(@NotNull PostEntity post);

    PostEntity archivePost(@NotNull PostEntity post);

    PostEntity restoreArchivedPost(@NotNull PostEntity post);

    PostEntity markPostDeleted(@NotNull PostEntity post);

    PostEntity restoreDeletedPost(@NotNull PostEntity post);

    PostEntity requirePendingPostForUpdate(@NotNull UUID postId);

    void deletePost(@NotNull PostEntity post);
}
