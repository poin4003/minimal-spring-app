package com.app.config.ratelimit;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.app.core.exception.ExceptionFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitSvc;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimited annotation = handlerMethod.getMethodAnnotation(RateLimited.class);
        if (annotation == null) {
            return true;
        }

        long retryAfterSeconds = rateLimitSvc.consume(annotation.value(), request);
        if (retryAfterSeconds > 0) {
            response.setHeader(
                    HttpHeaders.RETRY_AFTER,
                    Long.toString(retryAfterSeconds));
            throw ExceptionFactory.rateLimitExceeded(
                    "Too many requests. Please retry later.");
        }

        return true;
    }
}
