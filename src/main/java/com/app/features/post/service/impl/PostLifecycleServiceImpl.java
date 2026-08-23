package com.app.features.post.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.core.exception.ExceptionFactory;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.service.PostLifecycleService;

@Service
@Validated
public class PostLifecycleServiceImpl implements PostLifecycleService {

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
