package com.app.features.auth.cqrs.command;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.app.config.jwt.JwtTokenProvider;
import com.app.core.exception.ExceptionFactory;
import com.app.features.auth.cqrs.result.LoginResult;
import com.app.features.auth.entity.ConsumedRefreshTokenEntity;
import com.app.features.auth.entity.KeyStoreEntity;
import com.app.features.auth.repository.ConsumedRefreshTokenRepository;
import com.app.features.auth.repository.KeyStoreRepository;
import com.app.features.auth.service.AuthService;
import com.app.features.auth.service.KeyStoreService;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.repository.UserBaseRepository;

import an.awesome.pipelinr.Command;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

public record RefreshTokenCmd(String refreshToken) implements Command<LoginResult> {
}

@Slf4j
@Component
@RequiredArgsConstructor
class RefreshTokenHanlder implements Command.Handler<RefreshTokenCmd, LoginResult> {

    private final ConsumedRefreshTokenRepository consumedRefreshTokenRepo;
    private final KeyStoreService keyStoreService;
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final KeyStoreRepository keyStoreRepo;
    private final UserBaseRepository userBaseRepo;

    @Override
    public LoginResult handle(RefreshTokenCmd cmd) {
        ConsumedRefreshTokenEntity usedToken = consumedRefreshTokenRepo.findByTokenValue(cmd.refreshToken())
                .orElse(null);

        if (usedToken != null) {
            log.warn("Refresh Token reuse detected! userId: {}", usedToken.getUserId());

            keyStoreService.deleteKeyStoreByUserId(usedToken.getUserId());

            throw ExceptionFactory.permissionError("Something wrong happened! Please relogin.");
        }

        UUID userId = jwtTokenProvider.getUserIdFromTokenUnverified(cmd.refreshToken());
        if (userId == null)
            throw ExceptionFactory.notFound("userId " + userId);

        KeyStoreEntity keyStore = keyStoreRepo.findByUserId(userId)
                .orElseThrow(() -> ExceptionFactory.notFound("User keystore not found. Please login again."));

        if (!cmd.refreshToken().equals(keyStore.getRefreshToken())) {
            throw ExceptionFactory.permissionError("Invalid refresh token, please relogin!");
        }

        if (!jwtTokenProvider.validateToken(cmd.refreshToken(), keyStore.getPublicKey())) {
            throw ExceptionFactory.permissionError("RefreshToken Expired or Invalid Signature");
        }

        ConsumedRefreshTokenEntity history = new ConsumedRefreshTokenEntity();
        history.setKeyStoreId(keyStore.getId());
        history.setUserId(userId);
        history.setTokenValue(cmd.refreshToken());
        history.setUsedAt(Instant.now());

        Date expiryDate = jwtTokenProvider.getExpiryDateFromToken(cmd.refreshToken(), keyStore.getPublicKey());
        history.setExpiryDate(expiryDate.toInstant());

        consumedRefreshTokenRepo.save(history);

        UserBaseEntity user = userBaseRepo.findById(userId)
                .orElseThrow(() -> ExceptionFactory.notFound("User " + userId));

        return authService.generateAndSaveTokens(userId, user.getEmail());
    }
}