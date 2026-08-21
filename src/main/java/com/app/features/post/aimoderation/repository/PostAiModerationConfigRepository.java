package com.app.features.post.aimoderation.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.features.post.aimoderation.entity.PostAiModerationConfigEntity;

public interface PostAiModerationConfigRepository
        extends JpaRepository<PostAiModerationConfigEntity, UUID> {

    Optional<PostAiModerationConfigEntity> findByCode(String code);
}
