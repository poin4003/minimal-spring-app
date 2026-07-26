package com.app.features.notification.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.app.features.notification.entity.UserNotificationPreferenceEntity;
import com.app.features.notification.entity.UserNotificationPreferenceEntity_;

public interface UserNotificationPreferenceRepository
        extends JpaRepository<UserNotificationPreferenceEntity, UUID> {

    @EntityGraph(attributePaths = UserNotificationPreferenceEntity_.USER)
    Optional<UserNotificationPreferenceEntity>
            findByIdAndEmailEnabledTrue(UUID userId);
}
