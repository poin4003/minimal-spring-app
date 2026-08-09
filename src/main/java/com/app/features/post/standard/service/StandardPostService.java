package com.app.features.post.standard.service;

import java.util.UUID;

import org.springdoc.core.converters.models.Pageable;
import org.springframework.data.domain.Page;

import com.app.features.post.schema.payload.CreateStandardPostPayload;
import com.app.features.post.standard.entity.StandardPostEntity;
import com.app.features.post.standard.schema.filter.OwnerStandardPostFilterCriteria;
import com.app.features.post.standard.schema.result.OwnerStandardPostResult;
import com.app.features.post.standard.schema.result.PublicStandardPostResult;

import jakarta.validation.Valid;

public interface StandardPostService {

    OwnerStandardPostResult createStandardPost(UUID authorId, @Valid CreateStandardPostPayload payload);

    PublicStandardPostResult getPublishedPost(UUID postId);

    OwnerStandardPostResult getOwnerPost(UUID postId, UUID ownerId);

    StandardPostEntity requireStandardPost(UUID postId);

    void deleteOwnedPost(UUID postId, UUID ownerId);

    Page<PublicStandardPostResult> getPublishedPosts(
            UUID ownerId, OwnerStandardPostFilterCriteria criteria, Pageable pageable);

    Page<OwnerStandardPostResult> getOwnedPosts(
            UUID ownerId, OwnerStandardPostFilterCriteria criteria, Pageable pageable);
}
