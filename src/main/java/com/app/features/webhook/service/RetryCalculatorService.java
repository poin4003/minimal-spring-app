package com.app.features.webhook.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

@Service
public class RetryCalculatorService {

    public LocalDateTime calculateNextRetryAt(int attemptCount, int baseDelaySeconds, int maxDelaySeconds) {
        int safeBaseDelay = (baseDelaySeconds > 0) ? baseDelaySeconds : 30;
        int safeMaxDelay = (maxDelaySeconds > 0) ? maxDelaySeconds : 3600;

        long multiplier = (long) Math.pow(2, Math.max(0, attemptCount - 1));
        long delayInSeconds = safeBaseDelay * multiplier;

        delayInSeconds = Math.min(delayInSeconds, safeMaxDelay);

        return LocalDateTime.now().plusSeconds(delayInSeconds);
    }
}
