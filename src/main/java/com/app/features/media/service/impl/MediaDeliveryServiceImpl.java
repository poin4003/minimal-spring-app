package com.app.features.media.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.app.features.media.schema.result.MediaDeliveryResult;
import com.app.features.media.service.MediaDeliveryService;
import com.app.features.media.storage.MediaFileStorage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaDeliveryServiceImpl implements MediaDeliveryService {

    private static final Pattern HLS_SEGMENT_PATTERN =
            Pattern.compile("^segment-[0-9]{5}\\.ts$");
    private static final Pattern HLS_VARIANT_KEY_PATTERN =
            Pattern.compile("^[a-z0-9]+$");
    private static final String HLS_SEGMENT_CONTENT_TYPE = "video/mp2t";

    private final MediaRepository mediaRepository;
    private final MediaVariantRepository mediaVariantRepository;
    private final MediaFileStorage mediaFileStorage;

    @Override
    public MediaDeliveryResult getOriginal(String publicKey) {
        MediaEntity media = mediaRepository
                .findByPublicKeyAndStatusAndProcessingStatus(
                        publicKey,
                        RecordStatus.ACTIVE,
                        MediaProcessingStatus.READY)
                .orElseThrow(() -> ExceptionFactory.notFound("Media not found."));

        Path path = resolveReadableFile(media.getStorageKey());
        return new MediaDeliveryResult(
                path,
                media.getContentType(),
                media.getOriginalName(),
                media.getKind() == MediaKind.FILE);
    }

    @Override
    public MediaDeliveryResult getHlsManifest(String publicKey) {
        MediaVariantEntity playlist = getHlsVariant(
                publicKey,
                MediaVariantType.HLS_MASTER_PLAYLIST,
                HlsReservedVariantKey.MASTER.getKey());
        Path path = resolveReadableFile(playlist.getStorageKey());

        return new MediaDeliveryResult(
                path,
                playlist.getContentType(),
                null,
                false);
    }

    @Override
    public MediaDeliveryResult getHlsRendition(String publicKey, String variantKey) {
        MediaVariantEntity rendition = getHlsRenditionVariant(publicKey, variantKey);
        Path path = resolveReadableFile(rendition.getStorageKey());

        return new MediaDeliveryResult(
                path,
                rendition.getContentType(),
                null,
                false);
    }

    @Override
    public MediaDeliveryResult getHlsSegment(
            String publicKey,
            String variantKey,
            String segmentName) {
        if (segmentName == null || !HLS_SEGMENT_PATTERN.matcher(segmentName).matches()) {
            throw ExceptionFactory.notFound("Media stream segment not found.");
        }

        MediaVariantEntity rendition = getHlsRenditionVariant(publicKey, variantKey);
        String manifestKey = rendition.getStorageKey();
        int separatorIndex = manifestKey.lastIndexOf('/');
        if (separatorIndex < 0) {
            throw ExceptionFactory.notFound("Media stream not found.");
        }

        String segmentStorageKey = manifestKey.substring(0, separatorIndex + 1) + segmentName;
        Path path = resolveReadableFile(segmentStorageKey);
        return new MediaDeliveryResult(
                path,
                HLS_SEGMENT_CONTENT_TYPE,
                null,
                false);
    }

    private MediaVariantEntity getHlsRenditionVariant(String publicKey, String variantKey) {
        if (variantKey == null || !HLS_VARIANT_KEY_PATTERN.matcher(variantKey).matches()) {
            throw ExceptionFactory.notFound("Media stream rendition not found.");
        }
        return getHlsVariant(publicKey, MediaVariantType.HLS_RENDITION, variantKey);
    }

    private MediaVariantEntity getHlsVariant(
            String publicKey,
            MediaVariantType variantType,
            String variantKey) {
        return mediaVariantRepository
                .findByMedia_PublicKeyAndMedia_StatusAndMedia_ProcessingStatusAndVariantTypeAndVariantKey(
                        publicKey,
                        RecordStatus.ACTIVE,
                        MediaProcessingStatus.READY,
                        variantType,
                        variantKey)
                .orElseThrow(() -> ExceptionFactory.notFound("Media stream not found."));
    }

    private Path resolveReadableFile(String storageKey) {
        Path path = mediaFileStorage.resolve(storageKey);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw ExceptionFactory.notFound("Media file not found.");
        }
        return path;
    }
}
