package com.app.features.media.web.view;

import com.app.features.media.web.enums.MediaPreviewType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaPreviewModalView {

    private final String id;
    private final String title;
    private final String description;
    private final MediaPreviewType previewType;
    private final String sourceUrl;
    private final String originalUrl;
    private final String unavailableMessage;
}
