package com.app.features.webhook.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.features.webhook.entity.WebhookDeliveryAttemptEntity;
import com.app.features.webhook.enums.WebhookDeliveryAttemptStatus;

@Repository
public interface WebhookDeliveryAttemptRepository extends JpaRepository<WebhookDeliveryAttemptEntity, UUID> {

    @Query("SELECT a FROM WebhookDeliveryAttemptEntity a WHERE a.status = :status AND (a.nextRetryAt IS NULL OR a.nextRetryAt <= :now)")
    List<WebhookDeliveryAttemptEntity> findReadyToRun(@Param("status") WebhookDeliveryAttemptStatus status,
            @Param("now") LocalDateTime now);
}
