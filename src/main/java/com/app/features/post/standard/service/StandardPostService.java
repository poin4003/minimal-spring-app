package com.app.features.post.standard.service;

import java.util.UUID;

import com.app.features.post.schema.payload.CreateStandardPostPayload;
import com.app.features.post.standard.entity.StandardPostEntity;
import com.app.features.post.standard.schema.result.OwnerStandardPostResult;
import com.app.features.post.standard.schema.result.PublicStandardPostResult;

import jakarta.validation.Valid;

public interface StandardPostService {

    OwnerStandardPostResult createStandardPost(UUID authorId, @Valid CreateStandardPostPayload payload);

    PublicStandardPostResult getPublishedPost(UUID postId);

    OwnerStandardPostResult getOwnerPost(UUID postId, UUID ownerId);

    StandardPostEntity requireStandardPost(UUID postId);

    void deleteOwnedPost(UUID postId, UUID ownerId);
}
