package com.app.features.auth.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.config.jwt.JwtPayload;
import com.app.config.jwt.JwtTokenProvider;
import com.app.config.jwt.KeyPairDto;
import com.app.core.exception.ExceptionFactory;
import com.app.core.security.UserPrincipal;
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
import com.app.features.user.repository.UserBaseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserBaseRepository userBaseRepo;
    private final KeyStoreRepository keyStoreRepo;
    private final ConsumedRefreshTokenRepository consumedRefreshTokenRepo;
    private final KeyStoreService keyStoreService;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public LoginResult login(LoginPayload payload, String ipAddress) {
        String email = Objects.requireNonNull(payload.getEmail(), "Email must be not null");

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, payload.getPassword()));

        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        String userEmail = userDetails.getEmail();

        UserBaseEntity user = userBaseRepo.findByEmail(email)
                .orElseThrow(() -> ExceptionFactory.notFound("User: " + userId));

        user.setLoginTime(LocalDateTime.now());
        user.setLoginIp(ipAddress);
        userBaseRepo.save(user);

        return generateAndSaveTokens(userId, userEmail);
    }

    @Override
    @Transactional
    public LoginResult refreshToken(RefreshTokenPayload payload) {
        String tokenStr = payload.getRefreshToken();

        ConsumedRefreshTokenEntity usedToken = consumedRefreshTokenRepo.findByTokenValue(tokenStr)
                .orElse(null);

        if (usedToken != null) {
            log.warn("Refresh Token reuse detected! userId: {}", usedToken.getUserId());
            keyStoreService.deleteKeyStoreByUserId(usedToken.getUserId());
            throw ExceptionFactory.permissionError("Something wrong happened! Please relogin.");
        }

        UUID userId = jwtTokenProvider.getUserIdFromTokenUnverified(tokenStr);
        if (userId == null)
            throw ExceptionFactory.notFound("userId " + userId);

        KeyStoreEntity keyStore = keyStoreRepo.findByUserId(userId)
                .orElseThrow(() -> ExceptionFactory.notFound("User keystore not found. Please login again."));

        if (!tokenStr.equals(keyStore.getRefreshToken())) {
            throw ExceptionFactory.permissionError("Invalid refresh token, please relogin!");
        }

        if (!jwtTokenProvider.validateToken(tokenStr, keyStore.getPublicKey())) {
            throw ExceptionFactory.permissionError("RefreshToken Expired or Invalid Signature");
        }

        ConsumedRefreshTokenEntity history = new ConsumedRefreshTokenEntity();
        history.setKeyStoreId(keyStore.getId());
        history.setUserId(userId);
        history.setTokenValue(tokenStr);
        history.setUsedAt(Instant.now());

        Date expiryDate = jwtTokenProvider.getExpiryDateFromToken(tokenStr, keyStore.getPublicKey());
        history.setExpiryDate(expiryDate.toInstant());
        consumedRefreshTokenRepo.save(history);

        UserBaseEntity user = userBaseRepo.findById(userId)
                .orElseThrow(() -> ExceptionFactory.notFound("User " + userId));

        return generateAndSaveTokens(userId, user.getEmail());
    }

    @Override
    @Transactional
    public void logout(UUID keyStoreId, UUID userId) {
        UserBaseEntity user = userBaseRepo.findById(userId)
                .orElseThrow(() -> ExceptionFactory.notFound("User: " + userId));

        user.setLogoutTime(LocalDateTime.now());
        userBaseRepo.save(user);

        keyStoreRepo.deleteById(keyStoreId);
    }

    private LoginResult generateAndSaveTokens(UUID userId, String userEmail) {
        KeyPairDto newKeyPairDto = jwtTokenProvider.generateKeyPair();
        String newPrivateKey = newKeyPairDto.getPrivateKey();
        String newPublicKey = newKeyPairDto.getPublicKey();

        JwtPayload payload = JwtPayload.builder()
                .userEmail(userEmail)
                .build();

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, payload, newPrivateKey);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, newPrivateKey);

        Optional<KeyStoreEntity> existingOpt = keyStoreRepo.findByUserId(userId);

        if (existingOpt.isPresent()) {
            KeyStoreEntity existing = existingOpt.get();
            existing.setPublicKey(newPublicKey);
            existing.setPrivateKey(newPrivateKey);
            existing.setRefreshToken(newRefreshToken);
            keyStoreRepo.save(existing);
        } else {
            KeyStoreEntity keyStore = new KeyStoreEntity();
            keyStore.setUserId(userId);
            keyStore.setPublicKey(newPublicKey);
            keyStore.setPrivateKey(newPrivateKey);
            keyStore.setRefreshToken(newRefreshToken);
            keyStoreRepo.save(keyStore);
        }

        LoginResult response = new LoginResult();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);

        return response;
    }
}
