package com.app.features.post.service;

import java.util.UUID;

import com.app.features.post.entity.PostEntity;
import com.app.features.post.enums.PostType;
import com.app.features.user.entity.UserBaseEntity;

public interface PostService {

    PostEntity createPendingPost(UserBaseEntity author, PostType type);

    PostEntity requireOwnedPost(PostEntity post, UUID ownedId);

    void deletePost(PostEntity post);
}
