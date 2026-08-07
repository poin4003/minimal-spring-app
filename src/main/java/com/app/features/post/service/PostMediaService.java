package com.app.features.post.service;

import java.util.List;
import java.util.UUID;

import com.app.features.media.entity.MediaEntity;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.enums.PostMediaRole;

public interface PostMediaService {

    List<PostMediaEntity> findAttachments(UUID postId);
 
    List<PostMediaEntity> findAttachments(UUID postId, PostMediaRole role);

    List<PostMediaEntity> createAttachments(
            PostEntity post, PostMediaRole role, List<MediaEntity> orderedMedia);
}
