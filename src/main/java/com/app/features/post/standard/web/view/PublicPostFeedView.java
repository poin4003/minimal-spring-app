package com.app.features.post.standard.web.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicPostFeedView {

    public static final String ATTRIBUTE = "feed";

    private final String id;
    private final List<PublicPostCardView> posts;
    private final String nextPagePath;
}
