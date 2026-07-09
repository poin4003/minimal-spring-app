package com.app.features.webhook.service;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookRateLimiterService {

    private final ProxyManager<String> proxyManager;

    public boolean tryConsume(UUID endpointId, int maxRpm) {
        String key = "webhook_ratelimit:" + endpointId.toString();

        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(maxRpm)
                        .refillGreedy(maxRpm, Duration.ofMinutes(1))
                        .build())
                .build();

        boolean allowed = proxyManager.builder().build(key, configSupplier).tryConsume(1);

        if (!allowed) {
            log.warn("Rate limit exceeded for endpoint {}. Max RPM: {}", endpointId, maxRpm);
        }

        return allowed;
    }
}
