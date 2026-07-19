package com.app.features.auth.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.config.jwt.JwtAccessPayload;
import com.app.config.jwt.JwtTokenProvider;
import com.app.core.exception.ExceptionFactory;
import com.app.features.auth.entity.ConsumedRefreshTokenEntity;
import com.app.features.auth.entity.KeyStoreEntity;
import com.app.features.auth.repository.ConsumedRefreshTokenRepository;
import com.app.features.auth.repository.KeyStoreRepository;
import com.app.features.auth.schema.payload.LoginPayload;
import com.app.features.auth.schema.payload.RefreshTokenPayload;
import com.app.features.auth.schema.result.LoginResult;
import com.app.features.auth.service.AuthService;
import com.app.features.auth.service.KeyStoreService;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.enums.UserStatusEnum;
import com.app.features.user.repository.UserBaseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserBaseRepository userBaseRepo;
    private final KeyStoreRepository keyStoreRepo;
    private final ConsumedRefreshTokenRepository consumedRefreshTokenRepo;
    private final KeyStoreService keyStoreSvc;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public LoginResult login(LoginPayload payload, String ipAddress) {
        UserBaseEntity user = authenticateCredentials(payload);

        user.setLoginTime(LocalDateTime.now());
        user.setLoginIp(ipAddress);
        userBaseRepo.save(user);

        return createSessionTokens(user);
    }

    @Override
    @Transactional
    public LoginResult refreshToken(RefreshTokenPayload payload) {
        String tokenStr = payload.getRefreshToken();

        ConsumedRefreshTokenEntity usedToken = consumedRefreshTokenRepo.findByTokenValue(tokenStr)
                .orElse(null);

        if (usedToken != null) {
            log.warn("Refresh Token reuse detected! keyStoreId: {}", usedToken.getKeyStoreId());
            keyStoreSvc.deleteKeyStoreById(usedToken.getKeyStoreId());
            throw ExceptionFactory.permissionError("Something wrong happened! Please relogin.");
        }

        UUID keyStoreId = jwtTokenProvider.getKeyStoreIdFromTokenUnverified(tokenStr);
        if (keyStoreId == null) {
            throw ExceptionFactory.permissionError("Invalid refresh token, please relogin!");
        }

        KeyStoreEntity keyStore = keyStoreRepo.findById(keyStoreId)
                .orElseThrow(() -> ExceptionFactory.notFound("User keystore not found. Please login again."));

        UUID userId;
        try {
            userId = jwtTokenProvider.getUserId(tokenStr, keyStore.getSigningKey());
        } catch (Exception e) {
            throw ExceptionFactory.permissionError("RefreshToken Expired or Invalid Signature");
        }

        if (!userId.equals(keyStore.getUserId())) {
            throw ExceptionFactory.permissionError("Invalid refresh token, please relogin!");
        }

        if (!tokenStr.equals(keyStore.getRefreshToken())) {
            throw ExceptionFactory.permissionError("Invalid refresh token, please relogin!");
        }

        ConsumedRefreshTokenEntity history = new ConsumedRefreshTokenEntity();
        history.setKeyStoreId(keyStore.getId());
        history.setUserId(userId);
        history.setTokenValue(tokenStr);
        history.setUsedAt(Instant.now());

        Date expiryDate = jwtTokenProvider.getExpiryDateFromToken(tokenStr, keyStore.getSigningKey());
        history.setExpiryDate(expiryDate.toInstant());
        consumedRefreshTokenRepo.save(history);

        UserBaseEntity user = userBaseRepo.findWithAuthoritiesById(userId)
                .orElseThrow(() -> ExceptionFactory.notFound("User " + userId));

        if (user.getStatus() != UserStatusEnum.ACTIVE) {
            throw ExceptionFactory.invalidCredentials();
        }

        return rotateSessionTokens(keyStore, user);
    }

    @Override
    @Transactional
    public void logout(UUID userId, UUID keyStoreId) {
        UserBaseEntity user = userBaseRepo.findById(userId)
                .orElseThrow(() -> ExceptionFactory.notFound("User: " + userId));

        user.setLogoutTime(LocalDateTime.now());
        userBaseRepo.save(user);

        if (keyStoreId != null) {
            keyStoreRepo.deleteById(keyStoreId);
        }
    }

    private LoginResult createSessionTokens(UserBaseEntity user) {
        String signingKey = jwtTokenProvider.generateSigningKey();

        KeyStoreEntity keyStore = new KeyStoreEntity();
        keyStore.setUserId(user.getId());
        keyStore.setSigningKey(signingKey);
        keyStore = keyStoreRepo.save(keyStore);

        return issueTokens(keyStore, user, signingKey);
    }

    private UserBaseEntity authenticateCredentials(LoginPayload payload) {
        if (payload.getEmail() == null || payload.getPassword() == null) {
            throw ExceptionFactory.invalidCredentials();
        }

        UserBaseEntity user = userBaseRepo.findByEmail(payload.getEmail())
                .orElseThrow(() -> ExceptionFactory.invalidCredentials());

        if (!passwordEncoder.matches(payload.getPassword(), user.getPassword())) {
            throw ExceptionFactory.invalidCredentials();
        }

        if (user.getStatus() != UserStatusEnum.ACTIVE) {
            throw ExceptionFactory.invalidCredentials();
        }

        return user;
    }

    private LoginResult rotateSessionTokens(KeyStoreEntity keyStore, UserBaseEntity user) {
        String signingKey = jwtTokenProvider.generateSigningKey();
        keyStore.setSigningKey(signingKey);
        return issueTokens(keyStore, user, signingKey);
    }

    private LoginResult issueTokens(KeyStoreEntity keyStore, UserBaseEntity user, String signingKey) {
        JwtAccessPayload payload = buildAccessPayload(user);

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                keyStore.getUserId(),
                payload,
                keyStore.getId(),
                signingKey);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                keyStore.getUserId(),
                keyStore.getId(),
                signingKey);

        keyStore.setRefreshToken(newRefreshToken);
        keyStoreRepo.save(keyStore);

        LoginResult response = new LoginResult();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        return response;
    }

    private JwtAccessPayload buildAccessPayload(UserBaseEntity user) {
        Set<String> roleKeys = user.getRoles() == null
                ? Set.of()
                : user.getRoles().stream()
                        .map(role -> role.getKey())
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Set<String> permissionKeys = user.getRoles() == null
                ? Set.of()
                : user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(permission -> permission.getKey())
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return JwtAccessPayload.builder()
                .userEmail(user.getEmail())
                .status(user.getStatus())
                .roles(roleKeys)
                .permissions(permissionKeys)
                .build();
    }
}
