package com.app.config.ratelimit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RateLimitPolicy {
    AUTH_LOGIN(RateLimitSubject.CLIENT_IP),
    AUTH_REFRESH(RateLimitSubject.CLIENT_IP),
    REGISTRATION_OTP_IP(RateLimitSubject.CLIENT_IP),
    REGISTRATION_OTP_EMAIL(RateLimitSubject.EXPLICIT),
    REGISTRATION_VERIFY_IP(RateLimitSubject.CLIENT_IP),
    REGISTRATION_VERIFY_EMAIL(RateLimitSubject.EXPLICIT),
    REGISTRATION_COMPLETE_IP(RateLimitSubject.CLIENT_IP),
    PASSWORD_RESET_OTP_IP(RateLimitSubject.CLIENT_IP),
    PASSWORD_RESET_OTP_EMAIL(RateLimitSubject.EXPLICIT),
    PASSWORD_RESET_VERIFY_IP(RateLimitSubject.CLIENT_IP),
    PASSWORD_RESET_VERIFY_EMAIL(RateLimitSubject.EXPLICIT),
    PASSWORD_RESET_COMPLETE_IP(RateLimitSubject.CLIENT_IP),
    RAG_QUERY(RateLimitSubject.AUTHENTICATED_USER_OR_IP),
    MEDIA_DIRECT_UPLOAD(RateLimitSubject.AUTHENTICATED_USER),
    MEDIA_UPLOAD_SESSION(RateLimitSubject.AUTHENTICATED_USER),
    MEDIA_UPLOAD_CHUNK(RateLimitSubject.AUTHENTICATED_USER);

    private final RateLimitSubject subject;
}
