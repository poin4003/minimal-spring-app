package com.app.features.post.standard.web.view;

import java.util.List;
import java.util.UUID;

import com.app.features.post.schema.result.PostMediaResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostMediaGalleryModalView {

    public static final String ATTRIBUTE = "gallery";

    private final String id;
    private final String title;
    private final List<PostMediaResult> media;
    private final UUID activeMediaId;

    public boolean isActive(UUID mediaId, boolean first) {
        return activeMediaId == null
                ? first
                : activeMediaId.equals(mediaId);
    }
}
