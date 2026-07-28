package com.app.features.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.core.enums.AppLanguage;
import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.entity.UserInfoEntity_;

public interface UserInfoRepository extends JpaRepository<UserInfoEntity, UUID> {

    @EntityGraph(attributePaths = {
            UserInfoEntity_.USER,
            UserInfoEntity_.AVATAR_MEDIA
    })
    Optional<UserInfoEntity> findOneByUserId(UUID userId);

    @Query("""
            SELECT userInfo.language
            FROM UserInfoEntity userInfo
            WHERE userInfo.userId = :userId
            """)
    Optional<AppLanguage> findLanguageByUserId(
            @Param("userId") UUID userId);
}
