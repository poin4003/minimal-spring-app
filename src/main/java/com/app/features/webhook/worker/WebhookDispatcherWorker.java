package com.app.features.webhook.worker;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.app.features.webhook.entity.WebhookDeliveryAttemptEntity;
import com.app.features.webhook.entity.WebhookEventEntity;
import com.app.features.webhook.entity.WebhookSubscriptionEntity;
import com.app.features.webhook.enums.WebhookDeliveryAttemptStatus;
import com.app.features.webhook.handler.WebhookPartnerRule;
import com.app.features.webhook.handler.WebhookRuleFactory;
import com.app.features.webhook.repository.WebhookDeliveryAttemptRepository;
import com.app.features.webhook.repository.WebhookEventRepository;
import com.app.features.webhook.repository.WebhookSubscriptionRepository;
import com.app.features.webhook.service.RetryCalculatorService;
import com.app.features.webhook.service.WebhookRateLimiterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatcherWorker {

    private final WebhookRateLimiterService rateLimiter;
    private final RetryCalculatorService retryCalculator;
    private final WebhookDeliveryAttemptRepository attemptRepo;
    private final WebhookSubscriptionRepository subscriptionRepo;
    private final WebhookEventRepository eventRepo;
    private final WebhookRuleFactory ruleFactory;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void dispatchPendingWebhooks() {
        List<WebhookDeliveryAttemptEntity> pendingJobs = attemptRepo.findReadyToRun(
                WebhookDeliveryAttemptStatus.PENDING,
                LocalDateTime.now());

        if (pendingJobs.isEmpty()) {
            return;
        }

        for (WebhookDeliveryAttemptEntity attempt : pendingJobs) {
            try {
                processSingleAttempt(attempt);
            } catch (Exception e) {
                log.error("Fatal error processing attempt {}: {}", attempt.getId(), e.getMessage());
            }
        }
    }

    private void processSingleAttempt(WebhookDeliveryAttemptEntity attempt) {
        WebhookEventEntity event = eventRepo.findById(Objects.requireNonNull(attempt.getEvent().getId())).orElse(null);
        WebhookSubscriptionEntity subscription = subscriptionRepo
                .findById(Objects.requireNonNull(attempt.getSubscription().getId())).orElse(null);

        if (event == null || subscription == null) {
            log.error("Missing Event or Subscription for Attempt ID: {}", attempt.getId());
            attempt.setStatus(WebhookDeliveryAttemptStatus.EXHAUSTED);
            attempt.setLastErrorMessage("Missing Event or Subscription record in DB");
            attemptRepo.save(attempt);
            return;
        }

        if (!rateLimiter.tryConsume(subscription.getId(), subscription.getMaxRpm())) {
            return;
        }

        attempt.setAttemptCount(attempt.getAttemptCount() + 1);

        WebhookPartnerRule rule = ruleFactory.getRule(subscription.getPartnerCode());
        WebhookRuleResult result = rule.execute(event, subscription);

        attempt.setLastHttpStatus(result.getHttpStatusCode());
        attempt.setLastErrorMessage(result.getErrorMessage());

        switch (result.getDecision()) {
            case SUCCESS:
                attempt.setStatus(WebhookDeliveryAttemptStatus.SUCCESS);
                break;

            case FAIL_FAST:
                attempt.setStatus(WebhookDeliveryAttemptStatus.EXHAUSTED);
                break;

            case RETRY:
                if (attempt.getAttemptCount() >= subscription.getMaxRetries()) {
                    attempt.setStatus(WebhookDeliveryAttemptStatus.EXHAUSTED);
                } else {
                    attempt.setStatus(WebhookDeliveryAttemptStatus.PENDING);
                    attempt.setNextRetryAt(retryCalculator.calculateNextRetryAt(
                            attempt.getAttemptCount(), subscription.getBaseDelaySeconds(),
                            subscription.getMaxDelaySeconds()));
                }
                break;

            default:
                break;
        }

        attemptRepo.save(attempt);
    }
}
