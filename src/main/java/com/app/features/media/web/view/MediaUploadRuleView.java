package com.app.features.media.web.view;

import java.util.List;

import com.app.features.media.enums.MediaKind;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaUploadRuleView {

    private final String extension;
    private final MediaKind kind;
    private final long maxFileSizeBytes;
    private final List<String> contentTypes;
}
