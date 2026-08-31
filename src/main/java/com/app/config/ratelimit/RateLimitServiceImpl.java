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

        return consumeBucket(
                policy,
                resolveRequestSubject(policy, request));
    }

    @Override
    public long consume(
            RateLimitPolicy policy,
            String explicitSubject) {
        if (!properties.isEnabled()) {
            return 0;
        }

        if (policy.getSubject() != RateLimitSubject.EXPLICIT) {
            throw new IllegalArgumentException(
                    "Policy does not accept an explicit rate-limit subject: "
                            + policy);
        }
        if (explicitSubject == null || explicitSubject.isBlank()) {
            throw new IllegalArgumentException(
                    "Explicit rate-limit subject must not be blank.");
        }

        return consumeBucket(
                policy,
                "explicit:" + explicitSubject);
    }

    private long consumeBucket(
            RateLimitPolicy policy,
            String subject) {
        RateLimitProperties.Rule rule = properties.resolve(policy);
        String key = policy.name() + ":" + subject;

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

    private String resolveRequestSubject(
            RateLimitPolicy policy,
            HttpServletRequest request) {
        if (policy.getSubject() == RateLimitSubject.CLIENT_IP) {
            return resolveClientIpSubject(request);
        }

        if (policy.getSubject() == RateLimitSubject.EXPLICIT) {
            throw new IllegalArgumentException(
                    "Explicit rate-limit policy cannot be resolved from an HTTP request: "
                            + policy);
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal()
                        instanceof UserPrincipal principal) {
            return "user:" + principal.getUserId();
        }

        if (policy.getSubject()
                == RateLimitSubject.AUTHENTICATED_USER_OR_IP) {
            return resolveClientIpSubject(request);
        }

        if (policy.getSubject() == RateLimitSubject.AUTHENTICATED_USER) {
            throw ExceptionFactory.tokenInvalid("error.auth.required");
        }

        throw new IllegalArgumentException(
                "Unsupported HTTP rate-limit subject: "
                        + policy.getSubject());
    }

    private String resolveClientIpSubject(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        return "ip:"
                + (remoteAddress == null
                        ? UNKNOWN_CLIENT
                        : remoteAddress);
    }
}
