package com.app.features.media.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.app.core.enums.RecordStatus;
import com.app.core.exception.MyException;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.entity.MediaVariantEntity;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.enums.MediaVariantType;
import com.app.features.media.repository.MediaRepository;
import com.app.features.media.repository.MediaVariantRepository;
import com.app.features.media.schema.result.MediaDeliveryResult;
import com.app.features.media.storage.MediaFileStorage;

@ExtendWith(MockitoExtension.class)
class MediaDeliveryServiceImplTests {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MediaVariantRepository mediaVariantRepository;

    @Mock
    private MediaFileStorage mediaFileStorage;

    @TempDir
    private Path temporaryDirectory;

    private MediaDeliveryServiceImpl mediaDeliveryService;

    @BeforeEach
    void setUp() {
        mediaDeliveryService = new MediaDeliveryServiceImpl(
                mediaRepository,
                mediaVariantRepository,
                mediaFileStorage);
    }

    @Test
    void returnsReadyActiveOriginalMedia() throws Exception {
        Path originalPath = Files.writeString(temporaryDirectory.resolve("original.pdf"), "content");
        MediaEntity media = new MediaEntity();
        media.setStorageKey("2026/07/media/original.pdf");
        media.setOriginalName("guide.pdf");
        media.setContentType("application/pdf");
        media.setKind(MediaKind.DOCUMENT);

        given(mediaRepository.findByPublicKeyAndStatusAndProcessingStatus(
                "public-key",
                RecordStatus.ACTIVE,
                MediaProcessingStatus.READY))
                .willReturn(Optional.of(media));
        given(mediaFileStorage.resolve(media.getStorageKey())).willReturn(originalPath);

        MediaDeliveryResult result = mediaDeliveryService.getOriginal("public-key");

        assertThat(result.getPath()).isEqualTo(originalPath);
        assertThat(result.getContentType()).isEqualTo("application/pdf");
        assertThat(result.getFileName()).isEqualTo("guide.pdf");
        assertThat(result.isAttachment()).isFalse();
    }

    @Test
    void rejectsInvalidHlsSegmentBeforeStorageLookup() {
        assertThatThrownBy(() -> mediaDeliveryService.getHlsSegment(
                "public-key",
                "720p",
                "../original.mp4"))
                .isInstanceOf(MyException.class)
                .hasMessage("Media stream segment not found.");

        verifyNoInteractions(mediaVariantRepository, mediaFileStorage);
    }

    @Test
    void resolvesHlsSegmentBesideItsManifest() throws Exception {
        Path segmentPath = Files.writeString(temporaryDirectory.resolve("segment-00001.ts"), "segment");
        MediaVariantEntity playlist = new MediaVariantEntity();
        playlist.setStorageKey("2026/07/media/hls/720p/index.m3u8");

        given(mediaVariantRepository
                .findByMedia_PublicKeyAndMedia_StatusAndMedia_ProcessingStatusAndVariantTypeAndVariantKey(
                        "public-key",
                        RecordStatus.ACTIVE,
                        MediaProcessingStatus.READY,
                        MediaVariantType.HLS_RENDITION,
                        "720p"))
                .willReturn(Optional.of(playlist));
        given(mediaFileStorage.resolve("2026/07/media/hls/720p/segment-00001.ts"))
                .willReturn(segmentPath);

        MediaDeliveryResult result = mediaDeliveryService.getHlsSegment(
                "public-key",
                "720p",
                "segment-00001.ts");

        assertThat(result.getPath()).isEqualTo(segmentPath);
        assertThat(result.getContentType()).isEqualTo("video/mp2t");
    }

    @Test
    void returnsNotFoundWhenPhysicalFileIsMissing() {
        MediaEntity media = new MediaEntity();
        media.setStorageKey("2026/07/media/original.jpg");
        media.setOriginalName("image.jpg");
        media.setContentType("image/jpeg");
        media.setKind(MediaKind.IMAGE);

        given(mediaRepository.findByPublicKeyAndStatusAndProcessingStatus(
                "public-key",
                RecordStatus.ACTIVE,
                MediaProcessingStatus.READY))
                .willReturn(Optional.of(media));
        given(mediaFileStorage.resolve(media.getStorageKey()))
                .willReturn(temporaryDirectory.resolve("missing.jpg"));

        assertThatThrownBy(() -> mediaDeliveryService.getOriginal("public-key"))
                .isInstanceOf(MyException.class)
                .hasMessage("Media file not found.");
    }
}
