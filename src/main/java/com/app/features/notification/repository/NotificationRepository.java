package com.app.features.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.features.notification.entity.NotificationEntity;
import com.app.features.notification.entity.NotificationEntity_;
import com.app.features.notification.enums.NotificationResourceType;
import com.app.features.notification.enums.NotificationType;

public interface NotificationRepository
        extends JpaRepository<NotificationEntity, UUID>,
        JpaSpecificationExecutor<NotificationEntity> {

    @Override
    @EntityGraph(attributePaths = NotificationEntity_.ACTOR)
    Page<NotificationEntity> findAll(
            Specification<NotificationEntity> specification,
            Pageable pageable);

    @EntityGraph(attributePaths = NotificationEntity_.ACTOR)
    Optional<NotificationEntity> findByIdAndRecipient_Id(
            UUID notificationId,
            UUID recipientId);

    @EntityGraph(attributePaths = NotificationEntity_.ACTOR)
    Optional<NotificationEntity> findByRecipient_IdAndTypeAndResourceTypeAndResourceId(
            UUID recipientId,
            NotificationType type,
            NotificationResourceType resourceType,
            UUID resourceId);

    long deleteAllByResourceTypeAndResourceId(
            NotificationResourceType resourceType,
            UUID resourceId);

    long deleteAllByRecipient_IdAndTypeAndResourceTypeAndResourceId(
            UUID recipientId,
            NotificationType type,
            NotificationResourceType resourceType,
            UUID resourceId);

    List<NotificationEntity> findAllByRecipient_Id(
            UUID recipientId,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM NotificationEntity notification
            WHERE notification.createdAt < :cutoff
            """)
    int deleteExpiredNotifications(
            @Param("cutoff") LocalDateTime cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM notification
            WHERE id IN (
                SELECT ranked.id
                FROM (
                    SELECT
                        notification.id,
                        ROW_NUMBER() OVER (
                            PARTITION BY notification.recipient_id
                            ORDER BY
                                notification.created_at DESC,
                                notification.id DESC
                        ) AS row_number
                    FROM notification
                ) ranked
                WHERE ranked.row_number > :hardLimit
            )
            """, nativeQuery = true)
    int deleteOverflowNotifications(
            @Param("hardLimit") int hardLimit);

    long countByRecipient_IdAndReadAtIsNull(UUID recipientId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationEntity notification
            SET notification.readAt = :readAt,
                notification.updatedAt = :readAt
            WHERE notification.recipient.id = :recipientId
              AND notification.readAt IS NULL
            """)
    int markAllAsRead(
            @Param("recipientId") UUID recipientId,
            @Param("readAt") LocalDateTime readAt);
}
