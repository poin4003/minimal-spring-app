package com.app.features.notification.schema.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NotificationCleanupResult {

    private final long expiredCount;

    private final long overflowCount;
}
