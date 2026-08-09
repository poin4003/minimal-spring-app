package com.app.features.post.shortpost.web.view;

import java.util.List;

import com.app.features.ui.web.component.view.UiPaginationView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicShortFeedView {

    private final String id;
    private final List<PublicShortCardView> shorts;
    private final UiPaginationView pagination;
}
