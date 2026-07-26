package com.app.features.notification.service.impl;

import org.springframework.stereotype.Service;

import com.app.features.notification.schema.model.NotificationCleanupResult;
import com.app.features.notification.service.NotificationMaintenanceService;
import com.app.features.notification.service.NotificationPolicyService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationMaintenanceServiceImpl
        implements NotificationMaintenanceService {

    private final NotificationPolicyService notificationPolicySvc;

    @Override
    public NotificationCleanupResult cleanupNotifications() {
        long expiredCount =
                notificationPolicySvc.deleteExpiredNotifications();
        long overflowCount =
                notificationPolicySvc.deleteOverflowNotifications();

        return new NotificationCleanupResult(
                expiredCount,
                overflowCount);
    }
}
