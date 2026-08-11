package com.app.features.post.shortpost.web.view;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicShortDetailFeedView {

    public static final String ATTRIBUTE = "feed";

    private final String id;
    private final List<PublicShortCardView> shorts;
    private final UUID activePostId;
    private final String nextPagePath;
}
