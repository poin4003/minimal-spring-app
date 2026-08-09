package com.app.features.post.standard.web.view;

import java.util.List;

import com.app.features.ui.web.component.view.UiPaginationView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerPostWorkspaceView {

    private final String id;
    private final List<OwnerPostStatusFilterView> statusFilters;
    private final List<OwnerPostCardView> posts;
    private final UiPaginationView pagination;
}
