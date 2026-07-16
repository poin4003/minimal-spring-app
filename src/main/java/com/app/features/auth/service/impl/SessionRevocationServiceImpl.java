package com.app.features.auth.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.features.auth.repository.KeyStoreRepository;
import com.app.features.auth.service.SessionRevocationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionRevocationServiceImpl implements SessionRevocationService {

    private final KeyStoreRepository keyStoreRepo;

    @Override
    @Transactional
    public void revokeSessionsByUserId(UUID userId) {
        keyStoreRepo.deleteAllByUserId(userId);
    }

    @Override
    @Transactional
    public void revokeSessionsByRoleId(UUID roleId) {
        keyStoreRepo.deleteAllByRoleId(roleId);
    }
}
