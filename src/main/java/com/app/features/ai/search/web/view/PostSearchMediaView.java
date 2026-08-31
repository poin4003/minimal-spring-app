package com.app.features.ai.search.web.view;

import com.app.features.media.enums.MediaKind;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostSearchMediaView {

    private final MediaKind kind;
    private final String previewUrl;
    private final Integer originalWidth;
    private final Integer originalHeight;
}
