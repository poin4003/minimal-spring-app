package com.app.features.post.moderation.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.app.features.post.entity.PostEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public interface PostModerationCommandService {

    void publishPost(
            @NotNull UUID postId,
            @NotNull UUID moderatorId);

    Optional<PostEntity> publishPostAutomatically(
            @NotNull UUID postId,
            @NotNull LocalDateTime expectedUpdatedAt);

    void rejectPost(
            @NotNull UUID postId,
            @NotNull UUID moderatorId,
            @NotBlank @Size(max = 1_000) String reason);

    Optional<PostEntity> rejectPostAutomatically(
            @NotNull UUID postId,
            @NotNull LocalDateTime expectedUpdatedAt,
            @NotBlank @Size(max = 1_000) String reason);
}
