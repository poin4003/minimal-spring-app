package com.app.features.auth.service.impl;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.core.exception.ExceptionFactory;
import com.app.core.security.session.RevokeSessions;
import com.app.core.security.session.SessionRevocationScope;
import com.app.features.auth.service.AccountCredentialService;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.repository.UserBaseRepository;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class AccountCredentialServiceImpl
        implements AccountCredentialService {

    private final UserBaseRepository userBaseRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    @RevokeSessions(scope = SessionRevocationScope.USER)
    public void updatePassword(
            UUID userId,
            String password) {
        UserBaseEntity user = userBaseRepo
                .findOneById(userId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.user.notFound",
                        userId));

        user.setPassword(passwordEncoder.encode(password));
    }
}
