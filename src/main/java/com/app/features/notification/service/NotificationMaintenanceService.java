package com.app.features.notification.service;

import com.app.features.notification.schema.model.NotificationCleanupResult;

public interface NotificationMaintenanceService {

    NotificationCleanupResult cleanupNotifications();
}
