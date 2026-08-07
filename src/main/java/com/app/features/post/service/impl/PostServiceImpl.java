package com.app.features.post.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.app.core.exception.ExceptionFactory;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.enums.PostType;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.repository.PostRepository;
import com.app.features.post.service.PostService;
import com.app.features.user.entity.UserBaseEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepo;

    @Override
    public PostEntity createPendingPost(UserBaseEntity author, PostType type) {
        PostEntity post = new PostEntity();
        post.setAuthor(author);
        post.setType(type);
        post.setModerationStatus(PostModerationStatus.PENDING_REVIEW);

        return postRepo.save(post);
    }

    @Override
    public PostEntity requireOwnedPost(PostEntity post, UUID ownedId) {
        if (!post.getAuthor().getId().equals(ownedId)) {
            throw ExceptionFactory.notFound("error.post.notFound", post.getId());
        }

        return post;
    } 

    @Override
    public void deletePost(PostEntity post) {
        postRepo.delete(post);
    } 
}
