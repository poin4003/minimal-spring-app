package com.app.features.post.shortpost.web.view;

import java.util.List;

import com.app.features.post.web.view.OwnerPostStatusFilterView;
import com.app.features.ui.web.component.view.UiPaginationView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerShortWorkspaceView {

    private final String id;
    private final String refreshPath;
    private final String refreshEvent;
    private final List<OwnerPostStatusFilterView> statusFilters;
    private final List<OwnerShortCardView> shorts;
    private final UiPaginationView pagination;
}
