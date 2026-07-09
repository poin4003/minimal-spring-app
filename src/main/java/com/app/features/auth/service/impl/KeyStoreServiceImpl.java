package com.app.features.auth.service.impl;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.security.KeyStoreResult;
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
    public KeyStoreResult getKeyStoreByUserId(UUID userId) {
        return keyStoreRepo.findByUserId(userId)
                .map(keyStore -> modelMapper.map(keyStore, KeyStoreResult.class))
                .orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteKeyStoreByUserId(UUID userId) {
        keyStoreRepo.deleteByUserId(userId);
    }
}
