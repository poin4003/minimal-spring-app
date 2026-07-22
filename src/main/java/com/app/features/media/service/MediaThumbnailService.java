package com.app.features.media.service;

import java.util.Optional;

import com.app.features.media.entity.MediaEntity;
import com.app.features.media.schema.model.MediaThumbnailResult;

public interface MediaThumbnailService {

    Optional<MediaThumbnailResult> generateThumbnail(MediaEntity media);

    MediaThumbnailResult copyThumbnail(
            MediaEntity targetMedia,
            MediaEntity sourceMedia);
}
