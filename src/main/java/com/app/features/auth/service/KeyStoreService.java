package com.app.features.auth.service;

import java.util.UUID;

import com.app.core.security.KeyStoreResult;

public interface KeyStoreService {

    KeyStoreResult getKeyStoreById(UUID keyStoreId);

    void deleteKeyStoreById(UUID keyStoreId);
}
