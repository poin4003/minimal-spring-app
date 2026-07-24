package com.app.features.auth.service.impl;

import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.constant.CacheConstants;
import com.app.features.auth.repository.KeyStoreRepository;
import com.app.features.auth.service.SessionRevocationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionRevocationServiceImpl implements SessionRevocationService {

    private final KeyStoreRepository keyStoreRepo;

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConstants.KEY_STORE, allEntries = true)
    public void revokeSessionsByUserId(UUID userId) {
        keyStoreRepo.deleteAllByUserId(userId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConstants.KEY_STORE, allEntries = true)
    public void revokeSessionsByRoleId(UUID roleId) {
        keyStoreRepo.deleteAllByRoleId(roleId);
    }
}
