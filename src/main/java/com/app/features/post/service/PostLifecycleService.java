package com.app.features.post.service;

import java.time.LocalDateTime;

import com.app.features.post.entity.PostEntity;

import jakarta.validation.constraints.NotNull;

public interface PostLifecycleService {

    void archive(
            @NotNull PostEntity post,
            @NotNull LocalDateTime archivedAt);

    void restoreArchived(@NotNull PostEntity post);

    void softDelete(
            @NotNull PostEntity post,
            @NotNull LocalDateTime deletedAt);

    void restoreDeleted(@NotNull PostEntity post);
}
