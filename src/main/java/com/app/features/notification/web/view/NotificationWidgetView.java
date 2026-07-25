package com.app.features.notification.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationWidgetView {

    private final String inboxPath;
    private final String unreadCountPath;
}
