package com.app.features.ui.web.view;

import java.util.List;

import com.app.features.notification.web.view.NotificationWidgetView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialShellView {

    private final String title;
    private final String socialPath;
    private final String loginPath;
    private final String registrationPath;
    private final String logoutPath;
    private final String profilePath;
    private final String themeUpdatePath;
    private final String adminPath;
    private final UiCurrentUserView currentUser;
    private final List<SocialNavigationItemView> navigation;
    private final NotificationWidgetView notificationWidget;
}
