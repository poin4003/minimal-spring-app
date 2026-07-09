package com.app.features.webhook.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.app.features.webhook.entity.WebhookEventEntity;

@Repository
public interface WebhookEventRepository
        extends JpaRepository<WebhookEventEntity, UUID>, JpaSpecificationExecutor<WebhookEventEntity> {

}
