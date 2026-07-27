package com.app.features.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.entity.UserInfoEntity_;

public interface UserInfoRepository extends JpaRepository<UserInfoEntity, UUID> {

    @EntityGraph(attributePaths = {
            UserInfoEntity_.USER,
            UserInfoEntity_.AVATAR_MEDIA
    })
    Optional<UserInfoEntity> findOneByUserId(UUID userId);
}
