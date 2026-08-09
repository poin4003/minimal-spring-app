package com.app.features.post.standard.web.view;

import java.util.UUID;

import com.app.features.media.enums.MediaKind;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostComposerMediaItemView {

    private final UUID id;
    private final String originalName;
    private final MediaKind kind;
    private final String previewUrl;
    private final String iconClass;

    public boolean hasPreview() {
        return previewUrl != null;
    }
}
