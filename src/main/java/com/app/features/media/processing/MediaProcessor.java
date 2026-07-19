package com.app.features.media.processing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.app.config.settings.AppProperties;
import com.app.core.enums.RecordStatus;
import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.entity.MediaVariantEntity;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.enums.MediaVariantType;
import com.app.features.media.repository.MediaRepository;
import com.app.features.media.repository.MediaVariantRepository;
import com.app.features.media.storage.MediaFileStorage;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaProcessor {

    private static final String HLS_CONTENT_TYPE = "application/vnd.apple.mpegurl";

    private final MediaRepository mediaRepository;
    private final MediaVariantRepository mediaVariantRepository;
    private final MediaFileStorage mediaFileStorage;
    private final AppProperties appProperties;

    public void process(UUID mediaId) {
        MediaEntity media = prepareMedia(mediaId);
        if (media == null) {
            return;
        }

        try {
            String manifestKey = createHls(media);
            markReady(mediaId, manifestKey);
        } catch (RuntimeException ex) {
            markFailed(mediaId);
            throw ex;
        }
    }

    @Transactional
    private MediaEntity prepareMedia(UUID mediaId) {
        MediaEntity media = mediaRepository.findById(mediaId).orElse(null);
        if (media == null
                || media.getStatus() != RecordStatus.ACTIVE
                || media.getProcessingStatus() == MediaProcessingStatus.READY
                || !requiresHls(media.getKind())) {
            return null;
        }

        media.setProcessingStatus(MediaProcessingStatus.PENDING);
        return media;
    }

    private String createHls(MediaEntity media) {
        String mediaDirectoryKey = media.getStorageKey()
                .substring(0, media.getStorageKey().lastIndexOf('/'));
        String hlsDirectoryKey = mediaDirectoryKey + "/hls";
        String manifestKey = hlsDirectoryKey + "/index.m3u8";

        Path source = mediaFileStorage.resolve(media.getStorageKey());
        Path hlsDirectory = mediaFileStorage.resolve(hlsDirectoryKey);
        recreateDirectory(hlsDirectory);

        UrlOutput output = UrlOutput.toPath(mediaFileStorage.resolve(manifestKey))
                .setFormat("hls")
                .setCodec(StreamType.AUDIO, "aac")
                .addArguments("-hls_playlist_type", "vod")
                .addArguments("-hls_time", String.valueOf(
                        appProperties.getMedia().getFfmpeg().getSegmentDurationSeconds()))
                .addArguments(
                        "-hls_segment_filename",
                        hlsDirectory.resolve("segment-%05d.ts").toString());

        if (media.getKind() == MediaKind.VIDEO) {
            output.setCodec(StreamType.VIDEO, "libx264")
                    .addArguments("-preset", "veryfast");
        } else {
            output.disableStream(StreamType.VIDEO);
        }

        createFfmpeg()
                .addInput(UrlInput.fromPath(source))
                .addOutput(output)
                .setOverwriteOutput(true)
                .execute();

        return manifestKey;
    }

    private FFmpeg createFfmpeg() {
        Path configuredPath = Path.of(appProperties.getMedia().getFfmpeg().getExecutable());
        FFmpeg ffmpeg;
        if (Files.isDirectory(configuredPath)) {
            ffmpeg = FFmpeg.atPath(configuredPath);
        } else {
            Path binaryDirectory = configuredPath.getParent();
            ffmpeg = binaryDirectory == null
                    ? FFmpeg.atPath()
                    : FFmpeg.atPath(binaryDirectory);
        }

        long timeoutMillis = TimeUnit.MINUTES.toMillis(
                appProperties.getMedia().getFfmpeg().getProcessTimeoutMinutes());
        return ffmpeg.setExecutorTimeoutMillis(Math.toIntExact(timeoutMillis));
    }

    @Transactional
    private void markReady(UUID mediaId, String manifestKey) {
        MediaEntity media = mediaRepository.findById(mediaId).orElse(null);
        if (media == null) {
            return;
        }

        MediaVariantEntity variant = mediaVariantRepository
                .findByMedia_IdAndVariantType(mediaId, MediaVariantType.HLS_PLAYLIST)
                .orElseGet(() -> new MediaVariantEntity());
        variant.setMedia(media);
        variant.setVariantType(MediaVariantType.HLS_PLAYLIST);
        variant.setStorageKey(manifestKey);
        variant.setContentType(HLS_CONTENT_TYPE);
        mediaVariantRepository.save(variant);

        media.setProcessingStatus(MediaProcessingStatus.READY);
    }

    @Transactional
    private void markFailed(UUID mediaId) {
        mediaRepository.findById(mediaId)
                .ifPresent(media -> media.setProcessingStatus(MediaProcessingStatus.FAILED));
    }

    private boolean requiresHls(MediaKind kind) {
        return kind == MediaKind.VIDEO || kind == MediaKind.AUDIO;
    }

    private void recreateDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                List<Path> paths;
                try (var entries = Files.walk(directory)) {
                    paths = entries
                            .sorted((left, right) -> right.compareTo(left))
                            .toList();
                }
                for (Path path : paths) {
                    Files.deleteIfExists(path);
                }
            }
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to prepare HLS directory.");
        }
    }
}
