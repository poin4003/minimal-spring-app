package com.app.features.post.service.impl;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.core.exception.ExceptionFactory;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.enums.PostType;
import com.app.features.post.event.PostSubmittedForReviewEvent;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.repository.PostRepository;
import com.app.features.post.service.PostService;
import com.app.features.user.entity.UserBaseEntity;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepo;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PostEntity createDraftPost(UserBaseEntity author, PostType type) {
        PostEntity post = new PostEntity();
        post.setAuthor(author);
        post.setType(type);
        post.setLifecycleStatus(PostLifecycleStatus.DRAFT);
        post.setModerationStatus(null);

        return postRepo.save(post);
    }

    @Override
    public PostEntity requirePost(UUID postId) {
        return postRepo.findById(postId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.post.notFound",
                        postId));
    }

    @Override
    public PostEntity requireOwnedPost(PostEntity post, UUID ownerId) {
        if (!post.getAuthor().getId().equals(ownerId)) {
            throw ExceptionFactory.notFound("error.post.notFound", post.getId());
        }

        return post;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PostEntity prepareOwnedPostForUpdate(UUID postId, UUID ownerId) {
        PostEntity post = requireOwnedPostForUpdate(postId, ownerId);

        if (post.getLifecycleStatus() == PostLifecycleStatus.ARCHIVED
                || post.getLifecycleStatus() == PostLifecycleStatus.DELETED) {
            throw ExceptionFactory.invalidParam(
                    "error.post.lifecycleInvalid",
                    post.getId());
        }

        post.setLifecycleStatus(PostLifecycleStatus.DRAFT);
        clearModeration(post);

        return post;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PostEntity requireOwnedPostForUpdate(UUID postId, UUID ownerId) {
        PostEntity post = postRepo.findForUpdateById(postId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.post.notFound",
                        postId));

        requireOwnedPost(post, ownerId);

        return post;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void submitForReview(PostEntity post) {
        if (post.getLifecycleStatus() != PostLifecycleStatus.DRAFT) {
            throw ExceptionFactory.invalidParam(
                    "error.post.lifecycleInvalid",
                    post.getId());
        }

        post.setLifecycleStatus(PostLifecycleStatus.ACTIVE);
        post.setModerationStatus(PostModerationStatus.PENDING_REVIEW);
        post.setModerationSource(null);
        post.setPublishedAt(null);
        post.setModeratedBy(null);
        post.setModeratedAt(null);
        post.setRejectionReason(null);

        eventPublisher.publishEvent(
                new PostSubmittedForReviewEvent(post.getId()));
    }

    @Override
    public PostEntity requirePendingPost(PostEntity post) {
        if (!isPendingPost(post)) {
            throw ExceptionFactory.invalidParam(
                    "error.post.moderationInvalid",
                    post.getId());
        }

        return post;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PostEntity requirePendingPostForUpdate(UUID postId) {
        PostEntity post = postRepo.findForUpdateById(postId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.post.notFound",
                        postId));

        return requirePendingPost(post);
    }

    @Override
    public Optional<PostEntity> findPendingPost(UUID postId) {
        return postRepo.findById(postId)
                .filter(post -> isPendingPost(post));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<PostEntity> findPendingPostForUpdate(
            UUID postId,
            LocalDateTime expectedUpdatedAt) {
        return postRepo.findForUpdateById(postId)
                .filter(post -> isPendingPost(post))
                .filter(post -> Objects.equals(
                        post.getUpdatedAt(),
                        expectedUpdatedAt));
    }

    @Override
    public void deletePost(PostEntity post) {
        postRepo.delete(post);
    }

    private void clearModeration(PostEntity post) {
        post.setModerationStatus(null);
        post.setModerationSource(null);
        post.setPublishedAt(null);
        post.setModeratedBy(null);
        post.setModeratedAt(null);
        post.setRejectionReason(null);
    }

    private boolean isPendingPost(PostEntity post) {
        return post.getLifecycleStatus() == PostLifecycleStatus.ACTIVE
                && post.getModerationStatus()
                        == PostModerationStatus.PENDING_REVIEW;
    }
}
