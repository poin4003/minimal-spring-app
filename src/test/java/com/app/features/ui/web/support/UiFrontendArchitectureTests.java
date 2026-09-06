package com.app.features.ui.web.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class UiFrontendArchitectureTests {

    @Test
    void keepsReactiveStateOutOfHtmxAdapter() throws IOException {
        String uiScript = resource("/static/js/app-ui.js");
        String htmxScript = resource("/static/js/app-htmx.js");

        assertThat(uiScript)
                .contains("Alpine.store(\"theme\"")
                .contains("Alpine.store(\"navigation\"")
                .contains("Alpine.data(\"postComposer\"")
                .contains("Alpine.data(\"serverModalHost\"")
                .contains("Alpine.data(\"searchForm\"")
                .doesNotContain("addEventListener(\"htmx:");
        assertThat(htmxScript)
                .contains("addEventListener(\"htmx:")
                .contains("requestContexts")
                .contains("rejectStaleSwap")
                .contains("htmx:historyCacheMiss")
                .contains("syncHistoryBodyClass")
                .contains("data-app-body-class")
                .doesNotContain("event.detail.boosted")
                .doesNotContain("Alpine.data(")
                .doesNotContain("Alpine.store(");
    }

    @Test
    void loadsAlpineBeforeHtmxAdapter() throws IOException {
        String toolsTemplate = resource(
                "/templates/fragments/app-tools.html");

        assertThat(toolsTemplate.indexOf("/vendor/alpine/alpine.min.js"))
                .isGreaterThanOrEqualTo(0)
                .isLessThan(toolsTemplate.indexOf("/js/app-htmx.js"));
        assertThat(toolsTemplate)
                .contains("th:fragment=\"frontendScripts\"")
                .contains("th:fragment=\"coreHead\"")
                .contains("th:fragment=\"adminHead\"")
                .contains("th:fragment=\"socialHead\"")
                .contains("th:fragment=\"authScripts\"")
                .contains("\"historyCacheSize\":3")
                .contains("\"refreshOnHistoryMiss\":true")
                .contains("/js/media-preview.js")
                .contains("/js/media-upload.js")
                .doesNotContain("head-support")
                .doesNotContain("th:fragment=\"htmxScripts\"");
    }

    @Test
    void keepsMediaDependenciesOutOfAuthPages() throws IOException {
        String registerTemplate = resource(
                "/templates/auth/register.html");
        String forgotPasswordTemplate = resource(
                "/templates/auth/forgot-password.html");

        assertThat(registerTemplate)
                .contains("app-tools :: coreHead")
                .contains("app-tools :: authScripts")
                .doesNotContain("app-tools :: frontendScripts");
        assertThat(forgotPasswordTemplate)
                .contains("app-tools :: coreHead")
                .contains("app-tools :: authScripts")
                .doesNotContain("app-tools :: frontendScripts");
    }

    @Test
    void keepsFeatureLifecycleOutOfHtmxEvents() throws IOException {
        assertThat(resource("/static/js/media-preview.js"))
                .contains("Alpine.data(\"mediaPlayer\"")
                .doesNotContain("addEventListener(\"htmx:");
        assertThat(resource("/static/js/media-gallery.js"))
                .contains("Alpine.data(\"mediaHoverPreview\"")
                .doesNotContain("addEventListener(\"htmx:");
        assertThat(resource("/static/js/media-upload.js"))
                .contains("Alpine.data(\"mediaUpload\"")
                .doesNotContain("addEventListener(\"htmx:");
        assertThat(resource("/static/js/short-detail-feed.js"))
                .contains("Alpine.data(\"shortFeed\"")
                .doesNotContain("addEventListener(\"htmx:");
    }

    @Test
    void keepsShortDetailFocusedAndSharesQuickCreateUi()
            throws IOException {
        String shortDetail = resource(
                "/templates/post/short/public/detail.html");
        String standardList = resource(
                "/templates/post/standard/public/index.html");
        String shortList = resource(
                "/templates/post/short/public/index.html");
        String videoList = resource(
                "/templates/post/video/public/index.html");

        assertThat(shortDetail)
                .contains("post/short/public/fragments/detail-feed")
                .doesNotContain("components/breadcrumb");
        assertThat(standardList).contains("post/fragments/quick-create");
        assertThat(shortList).contains("post/fragments/quick-create");
        assertThat(videoList).contains("post/fragments/quick-create");
    }

    @Test
    void keepsSearchRequestsCompactAndReplaceable() throws IOException {
        String searchTemplate = resource(
                "/templates/ai/search/index.html");
        String searchStyles = resource("/static/css/post-search.css");

        assertThat(searchTemplate)
                .contains("hx-sync=\"this:replace\"")
                .doesNotContain("input-group input-group-lg");
        assertThat(searchStyles)
                .contains("width: min(100%, 46rem)")
                .contains("contain: layout paint");
    }

    @Test
    void linksPublicPostAvatarsToAuthorProfiles() throws IOException {
        assertThat(resource(
                "/templates/post/standard/public/fragments/card.html"))
                .contains("th:href=\"${card.authorPath}\"");
        assertThat(resource(
                "/templates/post/short/public/fragments/metadata.html"))
                .contains("th:href=\"${card.authorPath}\"");
        assertThat(resource(
                "/templates/post/video/public/detail.html"))
                .contains("th:href=\"${page.authorPath}\"");
    }

    @Test
    void scrollsWindowToTopForPrimarySocialNavigation() throws IOException {
        assertThat(resource(
                "/templates/fragments/social-navigation.html"))
                .contains("hx-swap=\"outerHTML show:window:top\"");
    }

    private String resource(String path) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assertThat(input).as(path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
