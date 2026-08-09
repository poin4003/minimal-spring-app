package com.app.features.post.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.config.settings.AppProperties;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.repository.PostRepository;
import com.app.features.post.schema.model.PostCleanupResult;
import com.app.features.post.service.PostMaintenanceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostMaintenanceServiceImpl implements PostMaintenanceService {

    private final PostRepository postRepo;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public PostCleanupResult cleanupExpiredPosts() {
        AppProperties.PostMaintenance maintenance = appProperties.getPost()
                .getMaintenance();
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = postRepo.deleteExpiredDeletedPosts(
                PostLifecycleStatus.DELETED,
                now.minus(maintenance.getDeletedRetention()));
        int rejectedCount = postRepo.deleteExpiredRejectedPosts(
                PostLifecycleStatus.ACTIVE,
                PostModerationStatus.REJECTED,
                now.minus(maintenance.getRejectedRetention()));

        return new PostCleanupResult(deletedCount, rejectedCount);
    }
}
