package com.app.features.post.service.impl;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.core.exception.ExceptionFactory;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.event.PostArchivedEvent;
import com.app.features.post.event.PostDeletedEvent;
import com.app.features.post.event.PostRestoredFromArchiveEvent;
import com.app.features.post.event.PostRestoredFromDeletionEvent;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.service.PostLifecycleService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class PostLifecycleServiceImpl implements PostLifecycleService {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void archive(
            PostEntity post,
            LocalDateTime archivedAt) {
        if (post.getLifecycleStatus() != PostLifecycleStatus.ACTIVE
                || post.getModerationStatus()
                != PostModerationStatus.PUBLISHED) {
            throw ExceptionFactory.invalidParam(
                    "error.post.lifecycleInvalid",
                    post.getId());
        }

        post.setLifecycleStatus(PostLifecycleStatus.ARCHIVED);
        post.setArchivedAt(archivedAt);
        eventPublisher.publishEvent(new PostArchivedEvent(post.getId()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void restoreArchived(PostEntity post) {
        if (post.getLifecycleStatus() != PostLifecycleStatus.ARCHIVED
                || post.getModerationStatus()
                != PostModerationStatus.PUBLISHED) {
            throw ExceptionFactory.invalidParam(
                    "error.post.lifecycleInvalid",
                    post.getId());
        }

        post.setLifecycleStatus(PostLifecycleStatus.ACTIVE);
        post.setArchivedAt(null);
        eventPublisher.publishEvent(
                new PostRestoredFromArchiveEvent(post.getId()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void softDelete(
            PostEntity post,
            LocalDateTime deletedAt) {
        if (post.getLifecycleStatus() == PostLifecycleStatus.DELETED) {
            throw ExceptionFactory.invalidParam(
                    "error.post.lifecycleInvalid",
                    post.getId());
        }

        post.setLifecycleStatus(PostLifecycleStatus.DELETED);
        post.setArchivedAt(null);
        post.setDeletedAt(deletedAt);
        eventPublisher.publishEvent(new PostDeletedEvent(post.getId()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void restoreDeleted(PostEntity post) {
        if (post.getLifecycleStatus() != PostLifecycleStatus.DELETED) {
            throw ExceptionFactory.invalidParam(
                    "error.post.lifecycleInvalid",
                    post.getId());
        }

        post.setLifecycleStatus(PostLifecycleStatus.DRAFT);
        post.setArchivedAt(null);
        post.setDeletedAt(null);
        clearModeration(post);
        eventPublisher.publishEvent(
                new PostRestoredFromDeletionEvent(post.getId()));
    }

    private void clearModeration(PostEntity post) {
        post.setModerationStatus(null);
        post.setModerationSource(null);
        post.setPublishedAt(null);
        post.setModeratedBy(null);
        post.setModeratedAt(null);
        post.setRejectionReason(null);
    }
}
