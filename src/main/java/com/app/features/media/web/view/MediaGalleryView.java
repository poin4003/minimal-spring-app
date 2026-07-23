package com.app.features.media.web.view;

import java.util.List;

import com.app.features.ui.web.component.view.UiPaginationView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaGalleryView {

    public static final String ATTRIBUTE = "gallery";

    private final String title;
    private final String description;
    private final String emptyMessage;
    private final String refreshPath;
    private final List<MediaGalleryItemView> items;
    private final UiPaginationView pagination;
}
