package com.app.config.ratelimit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "app.security.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    @Positive
    private long cacheMaxSize = 100_000;

    @NotNull
    private Duration cacheExpiration = Duration.ofHours(1);

    @Valid
    private final Rule authLogin = new Rule(10, Duration.ofMinutes(1));

    @Valid
    private final Rule authRefresh = new Rule(60, Duration.ofMinutes(1));

    @Valid
    private final Rule mediaDirectUpload = new Rule(30, Duration.ofMinutes(1));

    @Valid
    private final Rule mediaUploadSession = new Rule(120, Duration.ofMinutes(1));

    @Valid
    private final Rule mediaUploadChunk = new Rule(300, Duration.ofMinutes(1));

    public Rule resolve(RateLimitPolicy policy) {
        return switch (policy) {
            case AUTH_LOGIN -> authLogin;
            case AUTH_REFRESH -> authRefresh;
            case MEDIA_DIRECT_UPLOAD -> mediaDirectUpload;
            case MEDIA_UPLOAD_SESSION -> mediaUploadSession;
            case MEDIA_UPLOAD_CHUNK -> mediaUploadChunk;
        };
    }

    @AssertTrue(message = "Rate limit cache expiration must be positive.")
    public boolean isCacheExpirationValid() {
        return isPositive(cacheExpiration);
    }

    @Data
    public static class Rule {

        @Positive
        private long capacity;

        @NotNull
        private Duration period;

        public Rule() {
        }

        private Rule(long capacity, Duration period) {
            this.capacity = capacity;
            this.period = period;
        }

        @AssertTrue(message = "Rate limit period must be positive.")
        public boolean isPeriodValid() {
            return isPositive(period);
        }
    }

    private static boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
