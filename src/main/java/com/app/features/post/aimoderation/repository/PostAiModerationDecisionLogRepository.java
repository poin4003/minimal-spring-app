package com.app.features.post.aimoderation.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.app.features.post.aimoderation.entity.PostAiModerationDecisionLogEntity;

public interface PostAiModerationDecisionLogRepository
        extends JpaRepository<PostAiModerationDecisionLogEntity, UUID> {

    Page<PostAiModerationDecisionLogEntity> findAllByPost_Id(
            UUID postId,
            Pageable pageable);

    Optional<PostAiModerationDecisionLogEntity> findByIdAndPost_Id(
            UUID logId,
            UUID postId);
}
