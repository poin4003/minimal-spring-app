package com.app.features.notification.constant;

import java.util.Map;

import com.app.features.notification.enums.NotificationResourceType;

public final class NotificationSseEventNames {

    public static final String NOTIFICATION = "notification";

    public static final Map<NotificationResourceType, String> BY_RESOURCE_TYPE =
            Map.of(
                    NotificationResourceType.MEDIA,
                    "media");

    private NotificationSseEventNames() {
        throw new UnsupportedOperationException(
                "Constant class cannot be instantiated.");
    }
}
