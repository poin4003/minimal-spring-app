package com.app.features.ui.web.support;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.core.menu.MenuService;
import com.app.core.security.UserPrincipal;
import com.app.features.notification.web.view.NotificationWidgetView;
import com.app.features.ui.web.view.UiCurrentUserView;
import com.app.features.ui.web.view.UiShellView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UiShellFactory {

    private final AppProperties appProperties;
    private final MenuService menuSvc;

    public UiShellView build(
            UserPrincipal currentUser,
            String requestPath) {
        String notificationPath =
                appProperties.getUi().getHomePath() + "/notifications";

        return UiShellView.builder()
                .title(appProperties.getUi().getApplicationTitle())
                .logoutPath(appProperties.getUi().getLogoutPath())
                .profilePath(appProperties.getUi().getHomePath() + "/profile")
                .themeUpdatePath(
                        appProperties.getUi().getHomePath() + "/profile/theme")
                .currentUser(UiCurrentUserView.builder()
                        .email(currentUser.getEmail())
                        .authorities(currentUser.getAuthorities().stream()
                                .map(authority -> authority.getAuthority())
                                .toList())
                        .build())
                .menuTree(menuSvc.getMenuTree(requestPath))
                .notificationWidget(NotificationWidgetView.builder()
                        .inboxPath(notificationPath + "/inbox")
                        .unreadCountPath(notificationPath + "/unread-count")
                        .streamPath(notificationPath + "/stream")
                        .build())
                .build();
    }
}
