package com.app.features.post.shortpost.web.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicShortGalleryView {

    public static final String ATTRIBUTE = "gallery";

    private final String id;
    private final List<PublicShortCardView> shorts;
    private final String nextPagePath;
}
