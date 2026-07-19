package com.app.features.media.service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.features.media.entity.MediaEntity;
import com.app.features.media.schema.filter.MediaFilterCriteria;
import com.app.features.media.schema.payload.CreateMediaPayload;
import com.app.features.media.schema.result.MediaDetailResult;
import com.app.features.media.schema.result.MediaResult;

public interface MediaService {

    MediaResult createMedia(UUID createdById, CreateMediaPayload payload);

    void deleteOwnedMedia(UUID mediaId, UUID createdById);

    void deleteMedia(UUID mediaId);

    Page<MediaResult> getManyMedia(MediaFilterCriteria criteria, Pageable pageable);

    Page<MediaResult> getManyOwnedMedia(
            UUID ownerId,
            MediaFilterCriteria criteria,
            Pageable pageable);

    MediaDetailResult getMediaDetail(UUID mediaId);

    MediaDetailResult getOwnedMediaDetail(UUID mediaId, UUID ownerId);

    MediaResult retryProcessing(UUID mediaId);

    MediaResult retryOwnedProcessing(UUID mediaId, UUID ownerId);

    List<MediaEntity> requireOwnedActiveMedia(Collection<UUID> mediaIds, UUID createdById);
}
