package com.app.features.media.web.view;

import com.app.features.media.schema.result.MediaResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaUploadResultView {

    public static final String ATTRIBUTE = "result";

    private final MediaResult media;
    private final String message;
}
