package com.app.features.auth.security.session;

import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.app.core.security.session.RevokeSessions;
import com.app.core.security.session.SessionRevocationScope;
import com.app.features.auth.service.SessionRevocationService;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class SessionRevocationAspect {

    private final SessionRevocationService sessionRevocationService;
    private final TransactionTemplate transactionTemplate;

    @Around(
            value = "@annotation(revokeSessions) && args(targetId, ..)",
            argNames = "joinPoint,revokeSessions,targetId")
    public Object revokeSessions(
            ProceedingJoinPoint joinPoint,
            RevokeSessions revokeSessions,
            UUID targetId) {
        if (targetId == null) {
            throw new IllegalArgumentException("@RevokeSessions target ID must not be null.");
        }

        return transactionTemplate.execute(status -> {
            revokeSessions(revokeSessions.scope(), targetId);
            return proceed(joinPoint);
        });
    }

    private void revokeSessions(SessionRevocationScope scope, UUID targetId) {
        if (scope == SessionRevocationScope.USER) {
            sessionRevocationService.revokeSessionsByUserId(targetId);
            return;
        }

        sessionRevocationService.revokeSessionsByRoleId(targetId);
    }

    private Object proceed(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (RuntimeException | Error exception) {
            throw exception;
        } catch (Throwable exception) {
            throw new IllegalStateException("Session revocation target method failed.", exception);
        }
    }
}
