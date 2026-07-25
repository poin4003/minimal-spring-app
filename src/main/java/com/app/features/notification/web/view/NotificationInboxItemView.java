package com.app.features.notification.web.view;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationInboxItemView {

    private final UUID id;
    private final String title;
    private final String content;
    private final String createdAt;
    private final boolean unread;
    private final String markReadPath;
    private final String iconClass;
    private final String iconThemeClass;
}
