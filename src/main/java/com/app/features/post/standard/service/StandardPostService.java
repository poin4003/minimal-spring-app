package com.app.features.post.standard.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.features.post.schema.payload.CreateStandardPostPayload;
import com.app.features.post.schema.payload.UpdateStandardPostPayload;
import com.app.features.post.standard.entity.StandardPostEntity;
import com.app.features.post.standard.schema.filter.OwnerStandardPostFilterCriteria;
import com.app.features.post.standard.schema.filter.PublicStandardPostFilterCriteria;
import com.app.features.post.standard.schema.result.OwnerStandardPostResult;
import com.app.features.post.standard.schema.result.PublicStandardPostResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface StandardPostService {

    OwnerStandardPostResult createStandardPost(
            @NotNull UUID authorId,
            @NotNull @Valid CreateStandardPostPayload payload);

    OwnerStandardPostResult updateOwnedStandardPost(
            @NotNull UUID postId,
            @NotNull UUID ownerId,
            @NotNull @Valid UpdateStandardPostPayload payload);

    OwnerStandardPostResult submitOwnedPostForReview(
            @NotNull UUID postId,
            @NotNull UUID ownerId);

    PublicStandardPostResult getPublishedPost(@NotNull UUID postId);

    OwnerStandardPostResult getOwnerPost(
            @NotNull UUID postId,
            @NotNull UUID ownerId);

    StandardPostEntity requireStandardPost(@NotNull UUID postId);

    void deleteOwnedPost(
            @NotNull UUID postId,
            @NotNull UUID ownerId);

    Page<PublicStandardPostResult> getPublishedPosts(
            @NotNull PublicStandardPostFilterCriteria criteria,
            @NotNull Pageable pageable);

    Page<OwnerStandardPostResult> getOwnedPosts(
            @NotNull UUID ownerId,
            @NotNull OwnerStandardPostFilterCriteria criteria,
            @NotNull Pageable pageable);
}
