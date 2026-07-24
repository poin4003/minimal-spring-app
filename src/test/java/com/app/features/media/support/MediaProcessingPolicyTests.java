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
    void processesImagesVideosAndAudio() {
        assertThat(mediaProcessingPolicy.requiresProcessing(MediaKind.IMAGE))
                .isTrue();
        assertThat(mediaProcessingPolicy.requiresProcessing(MediaKind.VIDEO))
                .isTrue();
        assertThat(mediaProcessingPolicy.requiresProcessing(MediaKind.AUDIO))
                .isTrue();
    }

    @Test
    void leavesDocumentsAndFilesReadyImmediately() {
        assertThat(mediaProcessingPolicy.requiresProcessing(MediaKind.DOCUMENT))
                .isFalse();
        assertThat(mediaProcessingPolicy.requiresProcessing(MediaKind.FILE))
                .isFalse();
    }

    @Test
    void disablingThumbnailsDoesNotDisableHlsProcessing() {
        appProperties.getMedia().getThumbnail().setEnabled(false);

        assertThat(mediaProcessingPolicy.requiresProcessing(MediaKind.IMAGE))
                .isFalse();
        assertThat(mediaProcessingPolicy.requiresProcessing(MediaKind.VIDEO))
                .isTrue();
        assertThat(mediaProcessingPolicy.requiresProcessing(MediaKind.AUDIO))
                .isTrue();
    }
}
