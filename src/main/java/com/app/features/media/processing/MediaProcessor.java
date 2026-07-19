package com.app.features.media.processing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
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
import com.app.features.media.enums.HlsVariantProfile;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.enums.MediaVariantType;
import com.app.features.media.repository.MediaRepository;
import com.app.features.media.repository.MediaVariantRepository;
import com.app.features.media.storage.MediaFileStorage;
import com.app.features.media.validation.MediaProbe;
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
    private final MediaProbe mediaProbe;
    private final AppProperties appProperties;

    public void process(UUID mediaId) {
        MediaEntity media = prepareMedia(mediaId);
        if (media == null) {
            return;
        }

        try {
            List<HlsVariantProfile> generatedProfiles = createHls(media);
            markReady(mediaId, generatedProfiles);
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

    private List<HlsVariantProfile> createHls(MediaEntity media) {
        String mediaDirectoryKey = resolveMediaDirectoryKey(media.getStorageKey());
        String hlsDirectoryKey = mediaDirectoryKey + "/hls";

        Path source = mediaFileStorage.resolve(media.getStorageKey());
        Path hlsDirectory = mediaFileStorage.resolve(hlsDirectoryKey);
        recreateDirectory(hlsDirectory);

        List<HlsVariantProfile> profiles = media.getKind() == MediaKind.VIDEO
                ? resolveVideoProfiles(source)
                : List.of(HlsVariantProfile.AUDIO);

        for (HlsVariantProfile profile : profiles) {
            createHlsRendition(source, hlsDirectoryKey, profile, media.getKind());
        }
        writeMasterPlaylist(hlsDirectory.resolve("index.m3u8"), profiles);

        return profiles;
    }

    private void createHlsRendition(
            Path source,
            String hlsDirectoryKey,
            HlsVariantProfile profile,
            MediaKind mediaKind) {
        String renditionDirectoryKey = hlsDirectoryKey + "/" + profile.getKey();
        Path renditionDirectory = mediaFileStorage.resolve(renditionDirectoryKey);
        Path playlist = mediaFileStorage.resolve(renditionDirectoryKey + "/index.m3u8");

        try {
            Files.createDirectories(renditionDirectory);
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to prepare HLS rendition directory.");
        }

        UrlOutput output = UrlOutput.toPath(playlist)
                .setFormat("hls")
                .setCodec(StreamType.AUDIO, "aac")
                .addArguments("-b:a", String.valueOf(profile.getAudioBitrate()))
                .addArguments("-hls_playlist_type", "vod")
                .addArguments("-hls_time", String.valueOf(
                        appProperties.getMedia().getFfmpeg().getSegmentDurationSeconds()))
                .addArguments("-hls_flags", "independent_segments")
                .addArguments(
                        "-hls_segment_filename",
                        renditionDirectory.resolve("segment-%05d.ts").toString());

        if (mediaKind == MediaKind.VIDEO) {
            output.setCodec(StreamType.VIDEO, "libx264")
                    .setPixelFormat("yuv420p")
                    .addArguments("-vf", "scale=-2:" + profile.getHeight())
                    .addArguments("-b:v", String.valueOf(profile.getVideoBitrate()))
                    .addArguments("-maxrate", String.valueOf(profile.getVideoBitrate()))
                    .addArguments("-bufsize", String.valueOf(profile.getVideoBitrate() * 2))
                    .addArguments("-preset", "veryfast")
                    .addArguments("-sc_threshold", "0")
                    .addArguments(
                            "-force_key_frames",
                            "expr:gte(t,n_forced*"
                                    + appProperties.getMedia().getFfmpeg().getSegmentDurationSeconds()
                                    + ")");
        } else {
            output.disableStream(StreamType.VIDEO);
        }

        createFfmpeg()
                .addInput(UrlInput.fromPath(source))
                .addOutput(output)
                .setOverwriteOutput(true)
                .execute();
    }

    private List<HlsVariantProfile> resolveVideoProfiles(Path source) {
        int sourceHeight = mediaProbe.probe(source).getStreams().stream()
                .filter(stream -> StreamType.VIDEO.equals(stream.getCodecType()))
                .map(stream -> stream.getHeight())
                .filter(height -> height != null && height > 0)
                .mapToInt(height -> height)
                .max()
                .orElseThrow(() -> ExceptionFactory.serverError(
                        "Unable to determine video dimensions."));

        List<HlsVariantProfile> profiles = HlsVariantProfile.getVideoRenditions().stream()
                .filter(profile -> profile.getHeight() <= sourceHeight)
                .toList();
        return profiles.isEmpty()
                ? List.of(HlsVariantProfile.VIDEO_360P)
                : profiles;
    }

    private void writeMasterPlaylist(Path manifest, List<HlsVariantProfile> profiles) {
        StringBuilder content = new StringBuilder()
                .append("#EXTM3U\n")
                .append("#EXT-X-VERSION:3\n");

        for (HlsVariantProfile profile : profiles) {
            content.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(profile.getTotalBitrate())
                    .append('\n')
                    .append(profile.getKey())
                    .append("/index.m3u8\n");
        }

        try {
            Files.writeString(
                    manifest,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw ExceptionFactory.serverError("Unable to write HLS master playlist.");
        }
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
    private void markReady(UUID mediaId, List<HlsVariantProfile> generatedProfiles) {
        MediaEntity media = mediaRepository.findById(mediaId).orElse(null);
        if (media == null) {
            return;
        }

        String hlsDirectoryKey = resolveMediaDirectoryKey(media.getStorageKey()) + "/hls";
        List<MediaVariantEntity> variants = new ArrayList<>();
        variants.add(buildVariant(
                media,
                MediaVariantType.HLS_MASTER_PLAYLIST,
                HlsVariantProfile.MASTER,
                hlsDirectoryKey + "/index.m3u8"));

        for (HlsVariantProfile profile : generatedProfiles) {
            variants.add(buildVariant(
                    media,
                    MediaVariantType.HLS_RENDITION,
                    profile,
                    hlsDirectoryKey + "/" + profile.getKey() + "/index.m3u8"));
        }

        mediaVariantRepository.saveAll(variants);
        media.setProcessingStatus(MediaProcessingStatus.READY);
    }

    private MediaVariantEntity buildVariant(
            MediaEntity media,
            MediaVariantType variantType,
            HlsVariantProfile profile,
            String storageKey) {
        MediaVariantEntity variant = mediaVariantRepository
                .findByMedia_IdAndVariantTypeAndVariantKey(
                        media.getId(),
                        variantType,
                        profile.getKey())
                .orElseGet(() -> new MediaVariantEntity());
        variant.setMedia(media);
        variant.setVariantType(variantType);
        variant.setVariantKey(profile.getKey());
        variant.setStorageKey(storageKey);
        variant.setContentType(HLS_CONTENT_TYPE);
        variant.setWidth(null);
        variant.setHeight(profile.getHeight());
        variant.setBitrate(profile == HlsVariantProfile.MASTER
                ? null
                : profile.getTotalBitrate());
        return variant;
    }

    @Transactional
    private void markFailed(UUID mediaId) {
        mediaRepository.findById(mediaId)
                .ifPresent(media -> media.setProcessingStatus(MediaProcessingStatus.FAILED));
    }

    private boolean requiresHls(MediaKind kind) {
        return kind == MediaKind.VIDEO || kind == MediaKind.AUDIO;
    }

    private String resolveMediaDirectoryKey(String storageKey) {
        int separatorIndex = storageKey.lastIndexOf('/');
        if (separatorIndex < 0) {
            throw ExceptionFactory.serverError("Media storage key is invalid.");
        }
        return storageKey.substring(0, separatorIndex);
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
