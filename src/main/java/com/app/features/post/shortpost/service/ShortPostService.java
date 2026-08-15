package com.app.features.post.shortpost.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.shortpost.entity.ShortPostEntity;
import com.app.features.post.shortpost.schema.filter.OwnerShortPostFilterCriteria;
import com.app.features.post.shortpost.schema.filter.PublicShortPostFilterCriteria;
import com.app.features.post.shortpost.schema.payload.CreateShortPostPayload;
import com.app.features.post.shortpost.schema.payload.UpdateShortPostPayload;
import com.app.features.post.shortpost.schema.result.OwnerShortPostResult;
import com.app.features.post.shortpost.schema.result.PublicShortPostResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface ShortPostService {

    OwnerShortPostResult createShortPost(
            @NotNull UUID authorId,
            @NotNull @Valid CreateShortPostPayload payload);

    OwnerShortPostResult updateOwnedShortPost(
            @NotNull UUID postId,
            @NotNull UUID ownerId,
            @NotNull @Valid UpdateShortPostPayload payload);

    void submitOwnedPostForReview(
            @NotNull UUID postId,
            @NotNull UUID ownerId);

    PublicShortPostResult getPublishedPost(@NotNull UUID postId);

    OwnerShortPostResult getOwnerPost(
            @NotNull UUID postId,
            @NotNull UUID ownerId);

    Page<PublicShortPostResult> getPublishedPosts(
            @NotNull PublicShortPostFilterCriteria criteria,
            @NotNull Pageable pageable);

    Page<OwnerShortPostResult> getOwnedPosts(
            @NotNull UUID ownerId,
            @NotNull OwnerShortPostFilterCriteria criteria,
            @NotNull Pageable pageable);

    ShortPostEntity requireShortPost(@NotNull UUID postId);

    PostMediaEntity requireContentAttachment(@NotNull UUID postId);
}
