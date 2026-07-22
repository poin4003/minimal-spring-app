package com.app.features.media.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.app.config.settings.AppProperties;
import com.app.features.media.enums.MediaKind;

class MediaProcessingPolicyTests {

    private AppProperties appProperties;
    private MediaProcessingPolicy mediaProcessingPolicy;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        mediaProcessingPolicy = new MediaProcessingPolicy(appProperties);
    }

    @Test
    void processesImagesVideosAudioAndPdfDocuments() {
        assertThat(mediaProcessingPolicy.requiresProcessing(
                MediaKind.IMAGE,
                "image/jpeg"))
                .isTrue();
        assertThat(mediaProcessingPolicy.requiresProcessing(
                MediaKind.VIDEO,
                "video/mp4"))
                .isTrue();
        assertThat(mediaProcessingPolicy.requiresProcessing(
                MediaKind.AUDIO,
                "audio/mpeg"))
                .isTrue();
        assertThat(mediaProcessingPolicy.requiresProcessing(
                MediaKind.DOCUMENT,
                "application/pdf"))
                .isTrue();
    }

    @Test
    void leavesDirectDocumentsAndFilesReadyImmediately() {
        assertThat(mediaProcessingPolicy.requiresProcessing(
                MediaKind.DOCUMENT,
                "text/markdown"))
                .isFalse();
        assertThat(mediaProcessingPolicy.requiresProcessing(
                MediaKind.FILE,
                "application/zip"))
                .isFalse();
    }

    @Test
    void disablingThumbnailsDoesNotDisableHlsProcessing() {
        appProperties.getMedia().getThumbnail().setEnabled(false);

        assertThat(mediaProcessingPolicy.requiresProcessing(
                MediaKind.IMAGE,
                "image/png"))
                .isFalse();
        assertThat(mediaProcessingPolicy.requiresProcessing(
                MediaKind.VIDEO,
                "video/mp4"))
                .isTrue();
        assertThat(mediaProcessingPolicy.requiresProcessing(
                MediaKind.AUDIO,
                "audio/mpeg"))
                .isTrue();
    }
}
