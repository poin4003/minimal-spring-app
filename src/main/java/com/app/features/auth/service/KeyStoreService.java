package com.app.features.auth.service;

import java.util.UUID;

import com.app.core.security.KeyStoreResult;
import com.app.features.auth.entity.KeyStoreEntity;

public interface KeyStoreService {

    KeyStoreResult getKeyStoreById(UUID keyStoreId);

    void updateKeyStore(KeyStoreEntity keyStore);

    void deleteKeyStoreById(UUID keyStoreId);
}
