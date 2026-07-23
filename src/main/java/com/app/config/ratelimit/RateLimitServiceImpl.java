package com.app.config.ratelimit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.app.core.exception.ExceptionFactory;
import com.app.core.security.UserPrincipal;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class RateLimitServiceImpl implements RateLimitService {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final String UNKNOWN_CLIENT = "unknown";

    private final RateLimitProperties properties;
    private final Cache<String, Bucket> buckets;

    public RateLimitServiceImpl(RateLimitProperties properties) {
        this.properties = properties;
        this.buckets = Caffeine.newBuilder()
                .maximumSize(properties.getCacheMaxSize())
                .expireAfterAccess(properties.getCacheExpiration())
                .build();
    }

    @Override
    public long consume(RateLimitPolicy policy, HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return 0;
        }

        RateLimitProperties.Rule rule = properties.resolve(policy);
        String key = policy.name() + ":" + resolveSubject(policy, request);

        Bucket bucket = buckets.get(
                key,
                ignored -> Bucket.builder()
                        .addLimit(limit -> limit
                                .capacity(rule.getCapacity())
                                .refillGreedy(rule.getCapacity(), rule.getPeriod()))
                        .build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return 0;
        }

        return Math.max(
                1,
                Math.ceilDiv(
                        probe.getNanosToWaitForRefill(),
                        NANOS_PER_SECOND));
    }

    private String resolveSubject(
            RateLimitPolicy policy,
            HttpServletRequest request) {
        if (policy.getSubject() == RateLimitSubject.CLIENT_IP) {
            String remoteAddress = request.getRemoteAddr();
            return "ip:" + (remoteAddress == null ? UNKNOWN_CLIENT : remoteAddress);
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw ExceptionFactory.tokenInvalid("Authentication is required.");
        }

        return "user:" + principal.getUserId();
    }
}
