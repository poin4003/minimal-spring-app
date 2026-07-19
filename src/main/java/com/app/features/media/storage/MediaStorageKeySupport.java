package com.app.features.media.storage;

import com.app.core.exception.ExceptionFactory;

public final class MediaStorageKeySupport {

    private MediaStorageKeySupport() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String directoryOf(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw ExceptionFactory.invalidParam("Media storage key is invalid.");
        }

        int separatorIndex = storageKey.lastIndexOf('/');
        if (separatorIndex <= 0) {
            throw ExceptionFactory.invalidParam("Media storage key is invalid.");
        }
        return storageKey.substring(0, separatorIndex);
    }
}
