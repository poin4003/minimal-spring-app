package com.app.features.auth.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.app.config.jwt.JwtPayload;
import com.app.config.jwt.JwtTokenProvider;
import com.app.config.jwt.KeyPairDto;
import com.app.features.auth.cqrs.result.LoginResult;
import com.app.features.auth.entity.KeyStoreEntity;
import com.app.features.auth.repository.KeyStoreRepository;
import com.app.features.auth.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final KeyStoreRepository keyStoreRepo;

    public LoginResult generateAndSaveTokens(UUID userId, String userEmail) {
        KeyPairDto newKeyPairDto = jwtTokenProvider.generateKeyPair();
        String newPrivateKey = newKeyPairDto.getPrivateKey();
        String newPublicKey = newKeyPairDto.getPublicKey();

        JwtPayload payload = JwtPayload.builder()
                .userEmail(userEmail)
                .build();

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, payload, newPrivateKey);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, newPrivateKey);

        saveKeyStore(userId, newPublicKey, newPrivateKey, newRefreshToken);

        return LoginResult.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    private void saveKeyStore(UUID userId, String publicKey, String privateKey, String refreshToken) {
        Optional<KeyStoreEntity> existingOpt = keyStoreRepo.findByUserId(userId);

        if (existingOpt.isPresent()) {
            KeyStoreEntity existing = existingOpt.get();
            existing.setPublicKey(publicKey);
            existing.setPrivateKey(privateKey);
            existing.setRefreshToken(refreshToken);
            keyStoreRepo.save(existing);
        } else {
            KeyStoreEntity keyStore = new KeyStoreEntity();
            keyStore.setUserId(userId);
            keyStore.setPublicKey(publicKey);
            keyStore.setPrivateKey(privateKey);
            keyStore.setRefreshToken(refreshToken);

            keyStoreRepo.save(keyStore);
        }
    }
}
