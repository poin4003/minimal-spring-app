package com.app.features.media.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.config.settings.AppProperties;
import com.app.config.settings.AppProperties.HlsRendition;
import com.app.core.enums.RecordStatus;
import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.entity.MediaVariantEntity;
import com.app.features.media.enums.HlsReservedVariantKey;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.enums.MediaVariantType;
import com.app.features.media.repository.MediaRepository;
import com.app.features.media.repository.MediaVariantRepository;
import com.app.features.media.schema.model.HlsEncodingProfile;
import com.app.features.media.schema.model.HlsProcessingResult;
import com.app.features.media.schema.model.MediaThumbnailResult;
import com.app.features.media.service.MediaProcessingLeaseService;
import com.app.features.media.service.MediaProcessingService;
import com.app.features.media.service.MediaThumbnailService;
import com.app.features.media.storage.MediaFileStorage;
import com.app.features.media.storage.schema.MediaProcessingWorkspace;
import com.app.features.media.support.MediaFfmpegFactory;
import com.app.features.media.support.MediaProcessingPolicy;
import com.app.features.media.validation.MediaProbe;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaProcessingServiceImpl implements MediaProcessingService {

    private static final String HLS_CONTENT_TYPE = "application/vnd.apple.mpegurl";

    private final MediaRepository mediaRepo;
    private final MediaVariantRepository mediaVariantRepo;
    private final MediaFileStorage mediaFileStorage;
    private final MediaProbe mediaProbe;
    private final MediaThumbnailService mediaThumbnailSvc;
    private final MediaProcessingLeaseService mediaProcessingLeaseSvc;
    private final MediaFfmpegFactory mediaFfmpegFactory;
    private final MediaProcessingPolicy mediaProcessingPolicy;
    private final AppProperties appProperties;

    @Override
    public void process(UUID mediaId, UUID executionId) {
        if (!mediaProcessingLeaseSvc.acquire(mediaId, executionId)) {
            return;
        }

        try {
            MediaEntity media = prepareMedia(mediaId);
            if (media == null) {
                return;
            }

            try {
                boolean thumbnailAttempted = shouldCreateThumbnail(media);
                MediaThumbnailResult thumbnailResult = thumbnailAttempted
                        ? createThumbnail(media).orElse(null)
                        : null;
                HlsProcessingResult hlsResult = mediaProcessingPolicy
                        .requiresHls(media.getKind())
                                ? createHls(media)
                                : null;
                markReady(
                        mediaId,
                        hlsResult,
                        thumbnailResult,
                        thumbnailAttempted);
            } catch (RuntimeException ex) {
                markFailed(mediaId);
                throw ex;
            }
        } finally {
            mediaProcessingLeaseSvc.release(mediaId, executionId);
        }
    }

    @Transactional
    private MediaEntity prepareMedia(UUID mediaId) {
        MediaEntity media = mediaRepo.findById(mediaId).orElse(null);
        if (media == null
                || media.getStatus() != RecordStatus.ACTIVE
                || media.getProcessingStatus() == MediaProcessingStatus.READY) {
            return null;
        }

        media.setProcessingStatus(MediaProcessingStatus.PENDING);
        return media;
    }

    private Optional<MediaThumbnailResult> createThumbnail(MediaEntity media) {
        if (!mediaProcessingPolicy.shouldGenerateThumbnail(media)) {
            return Optional.empty();
        }

        try {
            Optional<MediaThumbnailResult> result = mediaThumbnailSvc
                    .generateThumbnail(media);
            if (result.isEmpty()
                    && mediaProcessingPolicy.isThumbnailRequired(media.getKind())) {
                throw ExceptionFactory.serverError("Required media thumbnail was not generated.");
            }
            return result;
        } catch (RuntimeException ex) {
            if (mediaProcessingPolicy.isThumbnailRequired(media.getKind())) {
                throw ex;
            }
            log.warn("Optional thumbnail generation failed for media [{}]", media.getId(), ex);
            return Optional.empty();
        }
    }

    private boolean shouldCreateThumbnail(MediaEntity media) {
        return (media.getThumbnailStorageKey() == null
                || media.getThumbnailStorageKey().isBlank())
                && mediaProcessingPolicy.shouldGenerateThumbnail(media);
    }

    private HlsProcessingResult createHls(MediaEntity media) {
        Path source = mediaFileStorage.resolve(media.getStorageKey());
        MediaProcessingWorkspace workspace =
                mediaFileStorage.prepareProcessingWorkspace(media.getStorageKey());

        try {
            List<HlsEncodingProfile> profiles = media.getKind() == MediaKind.VIDEO
                    ? resolveVideoProfiles(source)
                    : List.of(HlsEncodingProfile.audio(
                            appProperties.getMedia().getHls().getAudioBitrate()));

            for (HlsEncodingProfile profile : profiles) {
                createHlsRendition(
                        source,
                        workspace.getTemporaryDirectory(),
                        profile,
                        media.getKind());
            }
            writeMasterPlaylist(
                    workspace.getTemporaryDirectory().resolve("index.m3u8"),
                    profiles);

            mediaFileStorage.publishProcessingWorkspace(workspace);
            return new HlsProcessingResult(
                    workspace.getPublishedDirectoryKey(),
                    profiles);
        } finally {
            mediaFileStorage.discardProcessingWorkspace(workspace);
        }
    }

    private void createHlsRendition(
            Path source,
            Path hlsDirectory,
            HlsEncodingProfile profile,
            MediaKind mediaKind) {
        Path renditionDirectory = hlsDirectory.resolve(profile.getKey());
        Path playlist = renditionDirectory.resolve("index.m3u8");

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

        if (mediaKind == MediaKind.VIDEO && profile.isVideo()) {
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

        mediaFfmpegFactory.create()
                .addInput(UrlInput.fromPath(source))
                .addOutput(output)
                .setOverwriteOutput(true)
                .execute();
    }

    private List<HlsEncodingProfile> resolveVideoProfiles(Path source) {
        int sourceHeight = mediaProbe.probe(source).getStreams().stream()
                .filter(stream -> StreamType.VIDEO.equals(stream.getCodecType()))
                .map(stream -> stream.getHeight())
                .filter(height -> height != null && height > 0)
                .mapToInt(height -> height)
                .max()
                .orElseThrow(() -> ExceptionFactory.serverError(
                        "Unable to determine video dimensions."));

        List<HlsRendition> configuredProfiles = appProperties.getMedia()
                .getHls()
                .getRenditions()
                .stream()
                .sorted((left, right) -> Integer.compare(left.getHeight(), right.getHeight()))
                .toList();

        List<HlsRendition> selectedProfiles = configuredProfiles.stream()
                .filter(profile -> profile.getHeight() <= sourceHeight)
                .toList();
        List<HlsRendition> effectiveProfiles = selectedProfiles.isEmpty()
                ? List.of(configuredProfiles.getFirst())
                : selectedProfiles;

        return effectiveProfiles.stream()
                .map(profile -> HlsEncodingProfile.from(profile))
                .toList();
    }

    private void writeMasterPlaylist(Path manifest, List<HlsEncodingProfile> profiles) {
        StringBuilder content = new StringBuilder()
                .append("#EXTM3U\n")
                .append("#EXT-X-VERSION:3\n");

        for (HlsEncodingProfile profile : profiles) {
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

    @Transactional
    private void markReady(
            UUID mediaId,
            HlsProcessingResult hlsResult,
            MediaThumbnailResult thumbnailResult,
            boolean thumbnailAttempted) {
        MediaEntity media = mediaRepo.findById(mediaId).orElse(null);
        if (media == null) {
            return;
        }

        if (hlsResult != null) {
            mediaVariantRepo.deleteAllByMedia_Id(mediaId);
            mediaVariantRepo.flush();

            String hlsDirectoryKey = hlsResult.getPublishedDirectoryKey();
            List<MediaVariantEntity> variants = new ArrayList<>();

            MediaVariantEntity masterVariant = new MediaVariantEntity();
            masterVariant.setMedia(media);
            masterVariant.setVariantType(MediaVariantType.HLS_MASTER_PLAYLIST);
            masterVariant.setVariantKey(HlsReservedVariantKey.MASTER.getKey());
            masterVariant.setStorageKey(hlsDirectoryKey + "/index.m3u8");
            masterVariant.setContentType(HLS_CONTENT_TYPE);
            variants.add(masterVariant);

            for (HlsEncodingProfile profile : hlsResult.getProfiles()) {
                MediaVariantEntity renditionVariant = new MediaVariantEntity();
                renditionVariant.setMedia(media);
                renditionVariant.setVariantType(MediaVariantType.HLS_RENDITION);
                renditionVariant.setVariantKey(profile.getKey());
                renditionVariant.setStorageKey(
                        hlsDirectoryKey + "/" + profile.getKey() + "/index.m3u8");
                renditionVariant.setContentType(HLS_CONTENT_TYPE);
                renditionVariant.setHeight(profile.getHeight());
                renditionVariant.setBitrate(profile.getTotalBitrate());
                variants.add(renditionVariant);
            }

            mediaVariantRepo.saveAll(variants);
        }

        if (thumbnailAttempted) {
            media.setThumbnailStorageKey(thumbnailResult == null
                    ? null
                    : thumbnailResult.getStorageKey());
        }
        media.setProcessingStatus(MediaProcessingStatus.READY);
    }

    @Transactional
    private void markFailed(UUID mediaId) {
        mediaRepo.findById(mediaId)
                .ifPresent(media -> media.setProcessingStatus(MediaProcessingStatus.FAILED));
    }

}
