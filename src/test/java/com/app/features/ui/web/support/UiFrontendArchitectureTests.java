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
                .contains("/js/media-preview.js")
                .contains("/js/media-upload.js")
                .doesNotContain("head-support")
                .doesNotContain("th:fragment=\"htmxScripts\"");
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

    private String resource(String path) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assertThat(input).as(path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
