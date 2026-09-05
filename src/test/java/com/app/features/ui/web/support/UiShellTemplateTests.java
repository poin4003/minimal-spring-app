package com.app.features.ui.web.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.app.features.notification.web.view.NotificationWidgetView;
import com.app.features.ui.web.view.SocialShellView;
import com.app.features.ui.web.view.UiShellView;

class UiShellTemplateTests {

    private SpringTemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver =
                new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCacheable(false);

        templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    @Test
    void rendersAdminContentAndNavigationForHtmxRequest() {
        String html = renderAdminShell();

        assertThat(html)
                .doesNotContain("class=\"app-shell-layout\"")
                .doesNotContain("id=\"app-sidebar\"")
                .contains("id=\"app-page-content\"")
                .contains("id=\"app-sidebar-navigation\"");
    }

    @Test
    void rendersOnlySocialWorkspaceForHtmxRequest() {
        Context context = context(true);
        context.setVariable("shell", SocialShellView.builder()
                .socialTitle("Vibe")
                .socialPath("/posts")
                .loginPath("/login")
                .registrationPath("/register")
                .themeUpdatePath(null)
                .navigation(List.of())
                .build());

        String html = templateEngine.process(
                "test/social-shell",
                context);

        assertThat(html)
                .doesNotContain("class=\"social-shell-layout min-vh-100\"")
                .contains("id=\"app-social-workspace\"");
    }

    @Test
    void rendersDeclarativeAutoModalLauncher() {
        String html = templateEngine.process(
                "test/auto-modal",
                context(false));

        assertThat(html)
                .contains("x-data=\"autoModal\"")
                .contains("data-modal-id=\"sample-modal\"")
                .doesNotContain("DOMContentLoaded");
    }

    private String renderAdminShell() {
        Context context = context(true);
        context.setVariable("shell", UiShellView.builder()
                .title("Vibe")
                .logoutPath("/logout")
                .profilePath("/profile")
                .themeUpdatePath(null)
                .menuTree(List.of())
                .notificationWidget(NotificationWidgetView.builder()
                        .inboxPath("/notifications/inbox")
                        .unreadCountPath("/notifications/unread-count")
                        .streamPath("/notifications/stream")
                        .build())
                .build());

        return templateEngine.process("test/admin-shell", context);
    }

    private Context context(boolean htmxRequest) {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("htmxRequest", htmxRequest);
        return context;
    }
}
