package com.app.features.post.aimoderation.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.features.post.aimoderation.entity.PostAiModerationDecisionLogEntity;

public interface PostAiModerationDecisionLogRepository
        extends JpaRepository<PostAiModerationDecisionLogEntity, UUID> {
}
