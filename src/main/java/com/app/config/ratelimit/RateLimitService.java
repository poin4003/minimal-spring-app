package com.app.config.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

public interface RateLimitService {

    long consume(RateLimitPolicy policy, HttpServletRequest request);
}
