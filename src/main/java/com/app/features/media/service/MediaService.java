package com.app.features.media.service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.features.media.entity.MediaEntity;
import com.app.features.media.schema.filter.MediaFilterCriteria;
import com.app.features.media.schema.payload.CreateMediaPayload;
import com.app.features.media.schema.result.MediaResult;

public interface MediaService {

    MediaResult createMedia(UUID createdById, CreateMediaPayload payload);

    void deleteOwnedMedia(UUID mediaId, UUID createdById);

    void deleteMedia(UUID mediaId);

    Page<MediaResult> getManyMedia(MediaFilterCriteria criteria, Pageable pageable);

    List<MediaEntity> requireOwnedActiveMedia(Collection<UUID> mediaIds, UUID createdById);
}
