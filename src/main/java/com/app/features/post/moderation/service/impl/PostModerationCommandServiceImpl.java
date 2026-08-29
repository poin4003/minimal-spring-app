package com.app.features.post.moderation.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.features.post.entity.PostEntity;
import com.app.features.post.event.PostPublishedEvent;
import com.app.features.post.event.PostRejectedEvent;
import com.app.features.post.moderation.enums.PostModerationSource;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.moderation.service.PostModerationCommandService;
import com.app.features.post.service.PostMediaService;
import com.app.features.post.service.PostService;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class PostModerationCommandServiceImpl
        implements PostModerationCommandService {

    private final UserService userSvc;
    private final PostService postSvc;
    private final PostMediaService postMediaSvc;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void publishPost(UUID postId, UUID moderatorId) {
        PostEntity post = postSvc.requirePendingPostForUpdate(postId);
        postMediaSvc.requirePublishableMedia(post);
        applyPublish(
                post,
                userSvc.requireUser(moderatorId),
                PostModerationSource.MANUAL);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishPostDirectly(UUID postId) {
        PostEntity post = postSvc.requirePendingPostForUpdate(postId);
        postMediaSvc.requirePublishableMedia(post);
        applyPublish(post, null, PostModerationSource.DIRECT);
    }

    @Override
    @Transactional
    public Optional<PostEntity> publishPostAutomatically(
            UUID postId,
            LocalDateTime expectedUpdatedAt) {
        return postSvc.findPendingPostForUpdate(postId, expectedUpdatedAt)
                .map(post -> {
                    postMediaSvc.requirePublishableMedia(post);
                    applyPublish(
                            post,
                            null,
                            PostModerationSource.AI);
                    return post;
                });
    }

    @Override
    @Transactional
    public void rejectPost(
            UUID postId,
            UUID moderatorId,
            String reason) {
        PostEntity post = postSvc.requirePendingPostForUpdate(postId);
        applyRejection(
                post,
                userSvc.requireUser(moderatorId),
                reason,
                PostModerationSource.MANUAL);
    }

    @Override
    @Transactional
    public Optional<PostEntity> rejectPostAutomatically(
            UUID postId,
            LocalDateTime expectedUpdatedAt,
            String reason) {
        return postSvc.findPendingPostForUpdate(postId, expectedUpdatedAt)
                .map(post -> {
                    applyRejection(
                            post,
                            null,
                            reason,
                            PostModerationSource.AI);
                    return post;
                });
    }

    private void applyPublish(
            PostEntity post,
            UserBaseEntity moderator,
            PostModerationSource moderationSource) {
        LocalDateTime moderatedAt = LocalDateTime.now();
        post.setModerationStatus(PostModerationStatus.PUBLISHED);
        post.setModerationSource(moderationSource);
        post.setPublishedAt(moderatedAt);
        post.setModeratedBy(moderator);
        post.setModeratedAt(moderatedAt);
        post.setRejectionReason(null);
        eventPublisher.publishEvent(new PostPublishedEvent(post.getId()));
    }

    private void applyRejection(
            PostEntity post,
            UserBaseEntity moderator,
            String reason,
            PostModerationSource moderationSource) {
        post.setModerationStatus(PostModerationStatus.REJECTED);
        post.setModerationSource(moderationSource);
        post.setPublishedAt(null);
        post.setModeratedBy(moderator);
        post.setModeratedAt(LocalDateTime.now());
        post.setRejectionReason(reason.trim());
        eventPublisher.publishEvent(new PostRejectedEvent(post.getId()));
    }
}
