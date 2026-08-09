package com.app.features.ui.web.support;

import java.util.List;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.security.UserPrincipal;
import com.app.features.notification.web.view.NotificationWidgetView;
import com.app.features.ui.web.view.SocialNavigationItemView;
import com.app.features.ui.web.view.SocialShellView;
import com.app.features.ui.web.view.UiCurrentUserView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SocialShellFactory {

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final UiLandingPathResolver landingPathResolver;

    public SocialShellView build(
            UserPrincipal currentUser,
            String requestPath) {
        AppProperties.Ui ui = appProperties.getUi();
        boolean authenticated = currentUser != null;

        return SocialShellView.builder()
                .title(ui.getApplicationTitle())
                .socialPath(ui.getSocialPath())
                .loginPath(ui.getLoginPath())
                .registrationPath(ui.getRegistrationPath())
                .logoutPath(ui.getLogoutPath())
                .profilePath(authenticated ? ui.getProfilePath() : null)
                .themeUpdatePath(authenticated
                        ? ui.getProfilePath() + "/theme"
                        : null)
                .adminPath(landingPathResolver.canAccessCms(currentUser)
                        ? ui.getHomePath()
                        : null)
                .currentUser(authenticated ? toCurrentUser(currentUser) : null)
                .navigation(buildNavigation(requestPath))
                .notificationWidget(authenticated
                        ? buildNotificationWidget()
                        : null)
                .build();
    }

    private UiCurrentUserView toCurrentUser(UserPrincipal currentUser) {
        return UiCurrentUserView.builder()
                .email(currentUser.getEmail())
                .authorities(currentUser.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .toList())
                .build();
    }

    private List<SocialNavigationItemView> buildNavigation(
            String requestPath) {
        String socialPath = appProperties.getUi().getSocialPath();
        return List.of(SocialNavigationItemView.builder()
                .label(messageResolver.get("social.navigation.feed"))
                .icon("house-door")
                .path(socialPath)
                .active(matchesPath(socialPath, requestPath))
                .build());
    }

    private NotificationWidgetView buildNotificationWidget() {
        String notificationPath =
                appProperties.getUi().getNotificationPath();
        return NotificationWidgetView.builder()
                .inboxPath(notificationPath + "/inbox")
                .unreadCountPath(notificationPath + "/unread-count")
                .streamPath(notificationPath + "/stream")
                .build();
    }

    private boolean matchesPath(String itemPath, String requestPath) {
        if (itemPath.equals("/")) {
            return itemPath.equals(requestPath);
        }
        return requestPath != null
                && (requestPath.equals(itemPath)
                        || requestPath.startsWith(itemPath + "/"));
    }
}
