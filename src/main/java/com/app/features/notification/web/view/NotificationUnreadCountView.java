package com.app.features.notification.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationUnreadCountView {

    public static final String ATTRIBUTE = "notificationUnreadCount";

    private final long count;
    private final String refreshPath;
}
