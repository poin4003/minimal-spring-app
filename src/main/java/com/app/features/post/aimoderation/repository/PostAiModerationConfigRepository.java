package com.app.features.post.aimoderation.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.app.features.post.aimoderation.entity.PostAiModerationConfigEntity;

import jakarta.persistence.LockModeType;

public interface PostAiModerationConfigRepository
        extends JpaRepository<PostAiModerationConfigEntity, UUID> {

    Optional<PostAiModerationConfigEntity> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PostAiModerationConfigEntity> findForUpdateByCode(String code);
}
