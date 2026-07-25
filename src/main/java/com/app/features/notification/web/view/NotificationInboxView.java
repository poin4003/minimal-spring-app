package com.app.features.notification.web.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationInboxView {

    public static final String ATTRIBUTE = "notificationInbox";

    private final long unreadCount;
    private final List<NotificationInboxItemView> items;
    private final String markAllReadPath;
}
