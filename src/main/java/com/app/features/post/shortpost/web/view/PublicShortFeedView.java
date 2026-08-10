package com.app.features.post.shortpost.web.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicShortFeedView {

    public static final String ATTRIBUTE = "feed";

    private final String id;
    private final List<PublicShortCardView> shorts;
    private final String nextPagePath;
}
