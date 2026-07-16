package com.app.features.auth.service.impl;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.features.auth.repository.KeyStoreRepository;
import com.app.features.auth.service.SessionRevocationService;
import com.app.features.user.repository.UserBaseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionRevocationServiceImpl implements SessionRevocationService {

    private final KeyStoreRepository keyStoreRepo;
    private final UserBaseRepository userBaseRepo;

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> findUserIdsByRoleId(UUID roleId) {
        return userBaseRepo.findAllIdsByRoleId(roleId);
    }

    @Override
    @Transactional
    public void revokeSessionsByUserId(UUID userId) {
        keyStoreRepo.deleteAllByUserId(userId);
    }

    @Override
    @Transactional
    public void revokeSessionsByUserIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        keyStoreRepo.deleteAllByUserIds(userIds);
    }
}
