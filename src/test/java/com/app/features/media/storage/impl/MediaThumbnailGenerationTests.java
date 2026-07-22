package com.app.features.media.storage.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.app.config.settings.AppProperties;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.schema.model.MediaThumbnailResult;
import com.app.features.media.service.impl.MediaThumbnailServiceImpl;
import com.app.features.media.support.MediaFfmpegFactory;
import com.app.features.media.support.MediaProcessingPolicy;
import com.app.features.media.validation.MediaProbe;

class MediaThumbnailGenerationTests {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void rendersFirstPdfPageIntoBoundedJpegThumbnail() throws Exception {
        AppProperties appProperties = new AppProperties();
        appProperties.getMedia().setStoragePath(temporaryDirectory.toString());
        LocalMediaFileStorage mediaFileStorage = new LocalMediaFileStorage(appProperties);
        mediaFileStorage.initialize();

        String storageKey = "2026/07/media-id/original.pdf";
        Path original = temporaryDirectory.resolve(storageKey);
        Files.createDirectories(original.getParent());
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.A4));
            document.save(original.toFile());
        }

        MediaThumbnailServiceImpl mediaThumbnailSvc = new MediaThumbnailServiceImpl(
                mediaFileStorage,
                new MediaProbe(appProperties),
                new MediaFfmpegFactory(appProperties),
                new MediaProcessingPolicy(appProperties),
                appProperties);
        MediaEntity media = new MediaEntity();
        media.setStorageKey(storageKey);
        media.setKind(MediaKind.DOCUMENT);
        media.setContentType("application/pdf");

        MediaThumbnailResult result = mediaThumbnailSvc.generateThumbnail(media)
                .orElseThrow();
        Path thumbnail = mediaFileStorage.resolve(result.getStorageKey());
        BufferedImage image = ImageIO.read(thumbnail.toFile());

        assertThat(thumbnail).isRegularFile();
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isLessThanOrEqualTo(480);
        assertThat(image.getHeight()).isLessThanOrEqualTo(480);
        assertThat(result.getStorageKey()).isEqualTo("2026/07/media-id/thumbnail.jpg");
    }
}
