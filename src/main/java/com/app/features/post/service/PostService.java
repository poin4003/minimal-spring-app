package com.app.features.post.service;

import java.util.UUID;

import com.app.features.post.entity.PostEntity;
import com.app.features.post.enums.PostType;
import com.app.features.user.entity.UserBaseEntity;

import jakarta.validation.constraints.NotNull;

public interface PostService {

    PostEntity createPendingPost(
            @NotNull UserBaseEntity author,
            @NotNull PostType type);

    PostEntity requireOwnedPost(
            @NotNull PostEntity post,
            @NotNull UUID ownerId);

    PostEntity prepareOwnedPostForUpdate(
            @NotNull UUID postId,
            @NotNull UUID ownerId);

    PostEntity requirePendingPostForUpdate(@NotNull UUID postId);

    void deletePost(@NotNull PostEntity post);
}
