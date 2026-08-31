package com.app.config.ratelimit;

public enum RateLimitSubject {
    CLIENT_IP,
    AUTHENTICATED_USER,
    AUTHENTICATED_USER_OR_IP,
    EXPLICIT
}
