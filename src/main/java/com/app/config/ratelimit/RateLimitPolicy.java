package com.app.config.ratelimit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RateLimitPolicy {
    AUTH_LOGIN(RateLimitSubject.CLIENT_IP),
    AUTH_REFRESH(RateLimitSubject.CLIENT_IP),
    MEDIA_DIRECT_UPLOAD(RateLimitSubject.AUTHENTICATED_USER),
    MEDIA_UPLOAD_SESSION(RateLimitSubject.AUTHENTICATED_USER),
    MEDIA_UPLOAD_CHUNK(RateLimitSubject.AUTHENTICATED_USER);

    private final RateLimitSubject subject;
}
