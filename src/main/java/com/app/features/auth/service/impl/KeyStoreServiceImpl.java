package com.app.features.auth.service.impl;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.constant.CacheConstants;
import com.app.core.security.KeyStoreResult;
import com.app.features.auth.entity.KeyStoreEntity;
import com.app.features.auth.repository.KeyStoreRepository;
import com.app.features.auth.service.KeyStoreService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeyStoreServiceImpl implements KeyStoreService {

    private final KeyStoreRepository keyStoreRepo;
    private final ModelMapper modelMapper;

    @Override
    @Cacheable(
            cacheNames = CacheConstants.KEY_STORE,
            key = "#keyStoreId",
            sync = true)
    public KeyStoreResult getKeyStoreById(UUID keyStoreId) {
        return keyStoreRepo.findById(keyStoreId)
                .map(keyStore -> modelMapper.map(keyStore, KeyStoreResult.class))
                .orElse(null);
    }

    @Override
    @Transactional
    @CacheEvict(
            cacheNames = CacheConstants.KEY_STORE,
            key = "#keyStore.id")
    public void updateKeyStore(KeyStoreEntity keyStore) {
        keyStoreRepo.save(keyStore);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(
            cacheNames = CacheConstants.KEY_STORE,
            key = "#keyStoreId")
    public void deleteKeyStoreById(UUID keyStoreId) {
        keyStoreRepo.deleteById(keyStoreId);
    }
}
