package com.app.features.ui.web.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import com.app.features.notification.web.view.NotificationWidgetView;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.search.web.view.PostSearchItemView;
import com.app.features.ai.search.web.view.PostSearchMediaView;
import com.app.features.ai.search.web.view.PostSearchResultsView;
import com.app.features.ai.search.web.view.PostSearchSectionView;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.web.view.MediaUploadComponentView;
import com.app.features.media.web.view.MediaUploadRuleView;
import com.app.features.media.web.view.MediaUploadTransportView;
import com.app.features.post.enums.PostType;
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
    void rendersOnlyAdminContentForHtmxRequest() {
        String html = renderAdminShell(true);

        assertThat(html)
                .doesNotContain("class=\"app-shell-layout\"")
                .doesNotContain("id=\"app-sidebar\"")
                .contains("id=\"app-page-content\"")
                .contains("data-app-body-class=\"app-shell bg-body-tertiary\"")
                .contains("hx-history=\"false\"")
                .doesNotContain("id=\"app-sidebar-navigation\"");
    }

    @Test
    void rendersAdminPageContentOnlyOnceForRegularRequest() {
        String html = renderAdminShell(false);

        assertThat(html)
                .contains("class=\"app-shell-layout\"")
                .contains("id=\"app-sidebar\"")
                .containsOnlyOnce("id=\"app-page-content\"");
    }

    @Test
    void rendersOnlySocialPageContentForHtmxRequest() {
        String html = renderSocialShell(true);

        assertThat(html)
                .doesNotContain("class=\"social-shell-layout min-vh-100\"")
                .doesNotContain("id=\"app-social-workspace\"")
                .contains("id=\"app-social-page-content\"")
                .contains("data-app-body-class=\"bg-body-tertiary short-public-page\"")
                .contains("hx-history=\"false\"");
    }

    @Test
    void rendersSocialPageContentOnlyOnceForRegularRequest() {
        String html = renderSocialShell(false);

        assertThat(html)
                .contains("class=\"social-shell-layout min-vh-100\"")
                .contains("id=\"app-social-workspace\"")
                .containsOnlyOnce("id=\"app-social-page-content\"");
    }

    @Test
    void rendersSharedShellHeadAssetsOnce() {
        String adminHtml = templateEngine.process(
                "test/admin-head",
                context(false));
        String socialHtml = templateEngine.process(
                "test/social-head",
                context(false));

        assertThat(adminHtml)
                .containsOnlyOnce("name=\"viewport\"")
                .containsOnlyOnce("/js/app-ui.js")
                .containsOnlyOnce("/css/shell.css")
                .doesNotContain("/css/social-shell.css");
        assertThat(socialHtml)
                .containsOnlyOnce("name=\"viewport\"")
                .containsOnlyOnce("/js/app-ui.js")
                .containsOnlyOnce("/css/social-shell.css")
                .doesNotContain("/css/shell.css");
    }

    @Test
    void rendersSharedOwnerActions() {
        WebContext context = context(false);
        context.setVariable("card", Map.of(
                "editable", true,
                "editPath", "/my/posts/sample/edit",
                "actions", List.of(Map.of(
                        "modalPath", "/my/posts/sample/archive",
                        "buttonClass", "btn-outline-secondary",
                        "iconClass", "bi bi-archive",
                        "label", "Archive"))));

        String html = templateEngine.process(
                "test/owner-actions",
                context);

        assertThat(html)
                .contains("class=\"btn-group btn-group-sm test-group\"")
                .contains("href=\"/my/posts/sample/edit\"")
                .contains("hx-push-url=\"true\"")
                .contains("hx-get=\"/my/posts/sample/archive\"")
                .contains("hx-target=\"#app-modal-host\"")
                .contains("hx-push-url=\"false\"")
                .contains("btn-outline-secondary test-button");
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

    @Test
    void rendersSearchResultsOnTheServer() {
        WebContext context = context(false);
        context.setVariable("results", PostSearchResultsView.builder()
                .retrievalAvailability(AiAvailability.READY)
                .sections(List.of(PostSearchSectionView.builder()
                        .postType(PostType.VIDEO)
                        .title("Videos")
                        .items(List.of(PostSearchItemView.builder()
                                .content("Sample video")
                                .detailPath("/videos/sample")
                                .relevancePercent(95)
                                .media(List.of(PostSearchMediaView.builder()
                                        .kind(MediaKind.VIDEO)
                                        .previewUrl("/media/sample")
                                        .build()))
                                .build()))
                        .build()))
                .build());

        String html = templateEngine.process(
                "ai/search/fragments/results",
                context);

        assertThat(html)
                .contains("Videos")
                .contains("Sample video")
                .contains("href=\"/videos/sample\"")
                .contains("src=\"/media/sample\"");
    }

    @Test
    void rendersReactiveMediaUploadQueue() {
        WebContext context = context(false);
        context.setVariable("_csrf", new DefaultCsrfToken(
                "X-XSRF-TOKEN",
                "_csrf",
                "token"));
        context.setVariable("upload", MediaUploadComponentView.builder()
                .id("media-upload")
                .title("Upload")
                .description("Select files")
                .submitLabel("Upload queued")
                .accept("image/png")
                .multiple(true)
                .transport(MediaUploadTransportView.builder()
                        .directUploadPath("/media/upload")
                        .chunkUploadPath("/api/v1/media/uploads")
                        .directUploadThresholdBytes(1024)
                        .parallelChunks(2)
                        .build())
                .rules(List.of(MediaUploadRuleView.builder()
                        .extension("png")
                        .kind(MediaKind.IMAGE)
                        .maxFileSizeBytes(4096)
                        .contentTypes(List.of("image/png"))
                        .build()))
                .build());

        String html = templateEngine.process(
                "media/fragments/upload-modal",
                context);

        assertThat(html)
                .contains("x-data=\"mediaUpload\"")
                .contains("x-for=\"item in items\"")
                .contains("x-on:submit.prevent=\"uploadQueued\"")
                .doesNotContain("data-media-upload-item-template");
    }

    private String renderAdminShell(boolean htmxRequest) {
        WebContext context = context(htmxRequest);
        context.setVariable("_csrf", new DefaultCsrfToken(
                "X-XSRF-TOKEN",
                "_csrf",
                "token"));
        context.setVariable("shell", UiShellView.builder()
                .title("Motumo")
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

    private String renderSocialShell(boolean htmxRequest) {
        WebContext context = context(htmxRequest);
        context.setVariable("shell", SocialShellView.builder()
                .socialTitle("Motumo")
                .socialPath("/posts")
                .loginPath("/login")
                .registrationPath("/register")
                .themeUpdatePath(null)
                .navigation(List.of())
                .build());

        return templateEngine.process("test/social-shell", context);
    }

    private WebContext context(boolean htmxRequest) {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(
                servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(
                JakartaServletWebApplication
                        .buildApplication(servletContext)
                        .buildExchange(request, response),
                Locale.ENGLISH);
        context.setVariable("htmxRequest", htmxRequest);
        return context;
    }
}
