package com.app.features.webhook.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.app.features.webhook.entity.WebhookSubscriptionEntity;
import com.app.features.webhook.enums.WebhookSubscriptionStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookSubscriptionRepository
        extends JpaRepository<WebhookSubscriptionEntity, UUID>, JpaSpecificationExecutor<WebhookSubscriptionEntity> {

    Optional<WebhookSubscriptionEntity> findByPartnerCode(String code);

    List<WebhookSubscriptionEntity> findByPartnerIdAndStatus(UUID partnerId, WebhookSubscriptionStatus status);
}
