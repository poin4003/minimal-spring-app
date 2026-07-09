package com.app.features.webhook.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.app.core.utils.JsonNodeConverter;
import com.app.features.webhook.entity.WebhookDeliveryAttemptEntity;
import com.app.features.webhook.entity.WebhookEventEntity;
import com.app.features.webhook.entity.WebhookSubscriptionEntity;
import com.app.features.webhook.enums.WebhookDeliveryAttemptStatus;
import com.app.features.webhook.enums.WebhookSubscriptionStatus;
import com.app.features.webhook.repository.WebhookDeliveryAttemptRepository;
import com.app.features.webhook.repository.WebhookEventRepository;
import com.app.features.webhook.repository.WebhookSubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebhookPublisher {

    private final WebhookEventRepository eventRepo;
    private final WebhookSubscriptionRepository subscriptionRepo;
    private final WebhookDeliveryAttemptRepository attemptRepo;
    private final JsonNodeConverter jsonConverter;

    /**
     * Publish webhook event for partner
     * 
     * @param partner_id ID of partner, for noti
     * @param evetnType  type of event
     * @param reference  ex: order code, profile id
     * @param payload    object data, convert json before send
     */
    public void publishEvent(UUID partner_id, String eventType, String reference, Object payload) {
        List<WebhookSubscriptionEntity> subscriptions = subscriptionRepo.findByPartnerIdAndStatus(
                partner_id, WebhookSubscriptionStatus.ACTIVE);

        if (subscriptions.isEmpty()) {
            return;
        }

        WebhookEventEntity event = new WebhookEventEntity();
        event.setEventType(eventType);
        event.setBusinessReference(reference);
        event.setPayload(jsonConverter.toNode(payload));
        event = eventRepo.save(event);

        for (WebhookSubscriptionEntity sub : subscriptions) {
            WebhookDeliveryAttemptEntity attempt = new WebhookDeliveryAttemptEntity();
            attempt.setEvent(event);
            attempt.setSubscription(sub);
            attempt.setStatus(WebhookDeliveryAttemptStatus.PENDING);
            attempt.setAttemptCount(0);

            attemptRepo.save(attempt);
        }
    }
}
