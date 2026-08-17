package com.app.features.media.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.config.settings.AppProperties;
import com.app.config.settings.AppProperties.HlsRendition;
import com.app.core.enums.RecordStatus;
import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.entity.MediaVariantEntity;
import com.app.features.media.event.MediaProcessingFailedEvent;
import com.app.features.media.event.MediaReadyEvent;
import com.app.features.media.enums.HlsReservedVariantKey;
import com.app.features.media.enums.MediaHardwareAccel;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.enums.MediaVideoEncoder;
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
import com.github.kokorin.jaffree.ffprobe.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaProcessingServiceImpl implements MediaProcessingService {

    private static final String HLS_CONTENT_TYPE = "application/vnd.apple.mpegurl";
    private static final String QSV_DEVICE_NAME = "hw";
    private static final int QSV_EXTRA_HW_FRAMES = 64;

    private final MediaRepository mediaRepo;
    private final MediaVariantRepository mediaVariantRepo;
    private final MediaFileStorage mediaFileStorage;
    private final MediaProbe mediaProbe;
    private final MediaThumbnailService mediaThumbnailSvc;
    private final MediaProcessingLeaseService mediaProcessingLeaseSvc;
    private final MediaFfmpegFactory mediaFfmpegFactory;
    private final MediaProcessingPolicy mediaProcessingPolicy;
    private final AppProperties appProperties;
    private final ApplicationEventPublisher eventPublisher;

    private volatile Boolean hwEncodingAvailable;

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
                throw ExceptionFactory.serverError(
                        "error.media.requiredThumbnailMissing");
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
            Stream primaryVideoStream = media.getKind() == MediaKind.VIDEO
                    ? resolvePrimaryVideoStream(source)
                    : null;
            List<HlsEncodingProfile> profiles = primaryVideoStream != null
                    ? resolveVideoProfiles(primaryVideoStream)
                    : List.of(HlsEncodingProfile.audio(
                            appProperties.getMedia().getHls().getAudioBitrate()));
            MediaVideoEncoder preferredEncoder = resolveVideoEncoder();
            String videoDecoder = primaryVideoStream != null
                    ? resolveDecoderForEncoder(preferredEncoder, primaryVideoStream)
                    : null;

            for (HlsEncodingProfile profile : profiles) {
                createHlsRendition(
                        source,
                        workspace.getTemporaryDirectory(),
                        profile,
                        media.getKind(),
                        preferredEncoder,
                        videoDecoder);
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
            MediaKind mediaKind,
            MediaVideoEncoder preferredEncoder,
            String videoDecoder) {
        Path renditionDirectory = hlsDirectory.resolve(profile.getKey());
        Path playlist = renditionDirectory.resolve("index.m3u8");

        try {
            Files.createDirectories(renditionDirectory);
        } catch (IOException ex) {
            throw ExceptionFactory.serverError(
                    "error.media.hlsDirectoryPrepareFailed",
                    ex);
        }

        if (mediaKind == MediaKind.VIDEO && profile.isVideo()) {
            createVideoHlsRendition(
                    source,
                    playlist,
                    renditionDirectory,
                    profile,
                    preferredEncoder,
                    videoDecoder);
        } else {
            UrlOutput output = createBaseHlsOutput(playlist, renditionDirectory, profile);
            output.disableStream(StreamType.VIDEO);
            executeHls(source, output, null);
        }
    }

    private void createVideoHlsRendition(
            Path source,
            Path playlist,
            Path renditionDirectory,
            HlsEncodingProfile profile,
            MediaVideoEncoder preferredEncoder,
            String videoDecoder) {
        if (isHardwareEncoder(preferredEncoder)
                && Boolean.FALSE.equals(hwEncodingAvailable)
                && isSoftwareFallbackEnabled()) {
            preferredEncoder = MediaVideoEncoder.LIBX264;
        }

        switch (preferredEncoder) {
            case H264_VAAPI ->
                    executeVaapiRendition(
                            source, playlist, renditionDirectory, profile, videoDecoder);
            case H264_QSV ->
                    executeQsvRendition(
                            source, playlist, renditionDirectory, profile, videoDecoder);
            default ->
                    executeVideoHls(
                            source,
                            playlist,
                            renditionDirectory,
                            profile,
                            buildSoftwareVideoPlan(profile));
        }
    }

    private void executeVaapiRendition(
            Path source,
            Path playlist,
            Path renditionDirectory,
            HlsEncodingProfile profile,
            String vaapiDecoder) {
        RuntimeException decodeFailure = null;
        if (vaapiDecoder != null) {
            try {
                executeVideoHls(
                        source,
                        playlist,
                        renditionDirectory,
                        profile,
                        buildVaapiDecodeVideoPlan(profile, vaapiDecoder));
                hwEncodingAvailable = Boolean.TRUE;
                return;
            } catch (RuntimeException ex) {
                decodeFailure = ex;
                log.warn(
                        "VAAPI decode path failed for rendition [{}], retrying with upload path",
                        profile.getKey(),
                        ex);
            }
        }

        try {
            executeVideoHls(
                    source,
                    playlist,
                    renditionDirectory,
                    profile,
                    buildVaapiUploadVideoPlan(profile));
            hwEncodingAvailable = Boolean.TRUE;
        } catch (RuntimeException ex) {
            hwEncodingAvailable = Boolean.FALSE;
            if (!isSoftwareFallbackEnabled()) {
                if (decodeFailure != null) {
                    decodeFailure.addSuppressed(ex);
                    throw decodeFailure;
                }
                throw ex;
            }

            log.warn(
                    "Falling back to [{}] for HLS rendition [{}] after VAAPI path failed",
                    MediaVideoEncoder.LIBX264.getFfmpegName(),
                    profile.getKey(),
                    decodeFailure != null ? decodeFailure : ex);
            executeVideoHls(
                    source,
                    playlist,
                    renditionDirectory,
                    profile,
                    buildSoftwareVideoPlan(profile));
        }
    }

    private void executeQsvRendition(
            Path source,
            Path playlist,
            Path renditionDirectory,
            HlsEncodingProfile profile,
            String qsvDecoder) {
        RuntimeException qsvFailure = null;
        if (qsvDecoder != null) {
            try {
                executeVideoHls(
                        source,
                        playlist,
                        renditionDirectory,
                        profile,
                        buildQsvDecodeVideoPlan(profile, qsvDecoder));
                hwEncodingAvailable = Boolean.TRUE;
                return;
            } catch (RuntimeException ex) {
                qsvFailure = ex;
                log.warn(
                        "QSV decode path failed for rendition [{}], retrying with upload path",
                        profile.getKey(),
                        ex);
            }
        }

        try {
            executeVideoHls(
                    source,
                    playlist,
                    renditionDirectory,
                    profile,
                    buildQsvUploadVideoPlan(profile));
            hwEncodingAvailable = Boolean.TRUE;
        } catch (RuntimeException ex) {
            hwEncodingAvailable = Boolean.FALSE;
            if (!isSoftwareFallbackEnabled()) {
                if (qsvFailure != null) {
                    qsvFailure.addSuppressed(ex);
                    throw qsvFailure;
                }
                throw ex;
            }

            log.warn(
                    "Falling back to [{}] for HLS rendition [{}] after QSV path failed",
                    MediaVideoEncoder.LIBX264.getFfmpegName(),
                    profile.getKey(),
                    qsvFailure != null ? qsvFailure : ex);
            executeVideoHls(
                    source,
                    playlist,
                    renditionDirectory,
                    profile,
                    buildSoftwareVideoPlan(profile));
        }
    }

    private void executeVideoHls(
            Path source,
            Path playlist,
            Path renditionDirectory,
            HlsEncodingProfile profile,
            VideoExecutionPlan plan) {
        UrlOutput output = createBaseHlsOutput(playlist, renditionDirectory, profile);
        output.setCodec(StreamType.VIDEO, plan.videoEncoder().getFfmpegName())
                .addArguments(
                        "-vf",
                        plan.videoFilter())
                .addArguments("-b:v", String.valueOf(profile.getVideoBitrate()))
                .addArguments("-maxrate", String.valueOf(profile.getVideoBitrate()))
                .addArguments("-bufsize", String.valueOf(profile.getVideoBitrate() * 2));
        if (plan.videoEncoder() != MediaVideoEncoder.H264_VAAPI) {
            output.addArguments("-preset", "veryfast");
        }
        output.addArguments("-sc_threshold", "0")
                .addArguments(
                        "-force_key_frames",
                        "expr:gte(t,n_forced*"
                                + appProperties.getMedia().getFfmpeg().getSegmentDurationSeconds()
                                + ")");
        if (plan.applyPixelFormat()) {
            output.setPixelFormat("yuv420p");
        }
        executeHls(source, output, plan);
    }

    private UrlOutput createBaseHlsOutput(
            Path playlist,
            Path renditionDirectory,
            HlsEncodingProfile profile) {
        return UrlOutput.toPath(playlist)
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
    }

    private void executeHls(Path source, UrlOutput output, VideoExecutionPlan plan) {
        var ffmpeg = mediaFfmpegFactory.create();
        ffmpeg.addArguments("-loglevel", "level+info");

        int threads = appProperties.getMedia().getFfmpeg().getMachine().getThreads();
        if (threads <= 0 && plan != null && plan.inputDecoder() == null) {
            threads = mediaFfmpegFactory.calculateOptimalDecoderThreads();
        }
        if (threads > 0) {
            ffmpeg.addArguments("-threads", String.valueOf(threads));
        }

        UrlInput input = UrlInput.fromPath(source);
        if (plan != null) {
            switch (plan.hwDeviceType()) {
                case QSV ->
                        ffmpeg.addArguments("-init_hw_device", "qsv=" + QSV_DEVICE_NAME + ":auto_any")
                                .addArguments("-filter_hw_device", QSV_DEVICE_NAME);
                case VAAPI ->
                        ffmpeg.addArguments("-init_hw_device", "vaapi=hw:/dev/dri/renderD128")
                                .addArguments("-filter_hw_device", "hw");
                case NONE -> {
                }
            }
        }
        if (plan != null && plan.inputDecoder() != null) {
            switch (plan.hwDeviceType()) {
                case QSV ->
                        input.addArguments("-hwaccel", "qsv")
                                .addArguments("-hwaccel_output_format", "qsv")
                                .addArguments("-c:v", plan.inputDecoder());
                case VAAPI ->
                        input.addArguments("-hwaccel", "vaapi")
                                .addArguments("-hwaccel_output_format", "vaapi");
                case NONE -> {
                }
            }
        }

        ffmpeg.addInput(input)
                .addOutput(output)
                .setOverwriteOutput(true)
                .execute();
    }

    private MediaVideoEncoder resolveVideoEncoder() {
        AppProperties.Machine machine = appProperties.getMedia().getFfmpeg().getMachine();
        if (machine.getVideoEncoder() != MediaVideoEncoder.AUTO) {
            return machine.getVideoEncoder();
        }

        return switch (machine.getHardwareAccel()) {
            case QSV -> MediaVideoEncoder.H264_QSV;
            case VAAPI -> MediaVideoEncoder.H264_VAAPI;
            case NONE -> MediaVideoEncoder.LIBX264;
        };
    }

    private boolean isSoftwareFallbackEnabled() {
        return appProperties.getMedia().getFfmpeg().getMachine().isFallbackToSoftware();
    }

    private boolean isHardwareEncoder(MediaVideoEncoder encoder) {
        return encoder == MediaVideoEncoder.H264_QSV
                || encoder == MediaVideoEncoder.H264_VAAPI;
    }

    private VideoExecutionPlan buildSoftwareVideoPlan(HlsEncodingProfile profile) {
        return new VideoExecutionPlan(
                MediaVideoEncoder.LIBX264,
                null,
                "scale=" + profile.getWidth() + ":" + profile.getHeight(),
                MediaHardwareAccel.NONE,
                true);
    }

    private VideoExecutionPlan buildQsvDecodeVideoPlan(
            HlsEncodingProfile profile,
            String qsvDecoder) {
        return new VideoExecutionPlan(
                MediaVideoEncoder.H264_QSV,
                qsvDecoder,
                "scale_qsv=" + profile.getWidth() + ":" + profile.getHeight(),
                MediaHardwareAccel.QSV,
                false);
    }

    private VideoExecutionPlan buildQsvUploadVideoPlan(HlsEncodingProfile profile) {
        return new VideoExecutionPlan(
                MediaVideoEncoder.H264_QSV,
                null,
                "format=nv12,hwupload=extra_hw_frames="
                        + QSV_EXTRA_HW_FRAMES
                        + ",scale_qsv="
                        + profile.getWidth()
                        + ":"
                        + profile.getHeight(),
                MediaHardwareAccel.QSV,
                false);
    }

    private VideoExecutionPlan buildVaapiDecodeVideoPlan(
            HlsEncodingProfile profile,
            String vaapiDecoder) {
        return new VideoExecutionPlan(
                MediaVideoEncoder.H264_VAAPI,
                vaapiDecoder,
                "scale_vaapi=" + profile.getWidth() + ":" + profile.getHeight(),
                MediaHardwareAccel.VAAPI,
                false);
    }

    private VideoExecutionPlan buildVaapiUploadVideoPlan(HlsEncodingProfile profile) {
        return new VideoExecutionPlan(
                MediaVideoEncoder.H264_VAAPI,
                null,
                "format=nv12,hwupload,scale_vaapi="
                        + profile.getWidth()
                        + ":"
                        + profile.getHeight(),
                MediaHardwareAccel.VAAPI,
                false);
    }

    private String resolveVaapiDecoder(Stream videoStream) {
        String codecName = videoStream.getCodecName();
        if (codecName == null || codecName.isBlank()) {
            return null;
        }

        return switch (codecName.trim().toLowerCase(Locale.ROOT)) {
            case "h264", "hevc", "mpeg2video", "vp8", "vp9", "av1" ->
                    codecName.trim().toLowerCase(Locale.ROOT);
            default -> null;
        };
    }

    private Stream resolvePrimaryVideoStream(Path source) {
        return mediaProbe.probe(source).getStreams().stream()
                .filter(stream -> StreamType.VIDEO.equals(stream.getCodecType()))
                .filter(stream -> stream.getWidth() != null
                        && stream.getWidth() > 0
                        && stream.getHeight() != null
                        && stream.getHeight() > 0)
                .max((left, right) -> Integer.compare(
                        left.getHeight(),
                        right.getHeight()))
                .orElseThrow(() -> ExceptionFactory.serverError(
                        "error.media.videoDimensionsUndetermined"));
    }

    private List<HlsEncodingProfile> resolveVideoProfiles(Stream videoStream) {
        int sourceWidth = videoStream.getWidth();
        int sourceHeight = videoStream.getHeight();
        int sourceShortEdge = Math.min(sourceWidth, sourceHeight);

        List<HlsRendition> configuredProfiles = appProperties.getMedia()
                .getHls()
                .getRenditions()
                .stream()
                .sorted((left, right) -> Integer.compare(
                        left.getShortEdge(),
                        right.getShortEdge()))
                .toList();

        List<HlsRendition> selectedProfiles = configuredProfiles.stream()
                .filter(profile -> profile.getShortEdge() <= sourceShortEdge)
                .toList();
        List<HlsRendition> effectiveProfiles = selectedProfiles.isEmpty()
                ? List.of(configuredProfiles.getFirst())
                : selectedProfiles;

        return effectiveProfiles.stream()
                .map(profile -> HlsEncodingProfile.from(
                        profile,
                        sourceWidth,
                        sourceHeight))
                .toList();
    }

    private String resolveDecoderForEncoder(MediaVideoEncoder encoder, Stream videoStream) {
        if (isSoftwareDecodeCodec(videoStream)) {
            return null;
        }
        return switch (encoder) {
            case H264_QSV -> resolveQsvDecoder(videoStream);
            case H264_VAAPI -> resolveVaapiDecoder(videoStream);
            case AUTO, LIBX264 -> null;
        };
    }

    private boolean isSoftwareDecodeCodec(Stream videoStream) {
        String codecName = videoStream.getCodecName();
        if (codecName == null || codecName.isBlank()) {
            return false;
        }
        String normalized = codecName.trim().toLowerCase(Locale.ROOT);
        return appProperties.getMedia().getFfmpeg().getMachine()
                .getSoftwareDecodeCodecs()
                .stream()
                .anyMatch(codec -> codec.trim().toLowerCase(Locale.ROOT).equals(normalized));
    }

    private String resolveQsvDecoder(Stream videoStream) {
        String codecName = videoStream.getCodecName();
        if (codecName == null || codecName.isBlank()) {
            return null;
        }

        return switch (codecName.trim().toLowerCase(Locale.ROOT)) {
            case "h264" -> "h264_qsv";
            case "hevc" -> "hevc_qsv";
            case "vp9" -> "vp9_qsv";
            case "vp8" -> "vp8_qsv";
            case "av1" -> "av1_qsv";
            case "mpeg2video" -> "mpeg2_qsv";
            case "mjpeg" -> "mjpeg_qsv";
            default -> null;
        };
    }

    private void writeMasterPlaylist(Path manifest, List<HlsEncodingProfile> profiles) {
        StringBuilder content = new StringBuilder()
                .append("#EXTM3U\n")
                .append("#EXT-X-VERSION:3\n");

        for (HlsEncodingProfile profile : profiles) {
            content.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(profile.getTotalBitrate());

            if (profile.isVideo()) {
                content.append(",RESOLUTION=")
                        .append(profile.getWidth())
                        .append('x')
                        .append(profile.getHeight());
            }

            content.append('\n')
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
            throw ExceptionFactory.serverError(
                    "error.media.hlsMasterWriteFailed",
                    ex);
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
                renditionVariant.setWidth(profile.getWidth());
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

        eventPublisher.publishEvent(new MediaReadyEvent(
                media.getId(),
                media.getCreatedBy().getId(),
                media.getOriginalName()));
    }

    @Transactional
    private void markFailed(UUID mediaId) {
        mediaRepo.findById(mediaId).ifPresent(media -> {
            media.setProcessingStatus(MediaProcessingStatus.FAILED);
            eventPublisher.publishEvent(new MediaProcessingFailedEvent(
                    media.getId(),
                    media.getCreatedBy().getId(),
                    media.getOriginalName()));
        });
    }

    private record VideoExecutionPlan(
            MediaVideoEncoder videoEncoder,
            String inputDecoder,
            String videoFilter,
            MediaHardwareAccel hwDeviceType,
            boolean applyPixelFormat) {
    }
}
