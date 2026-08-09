package com.app.features.post.standard.web.view;

import java.util.List;

import com.app.features.ui.web.component.view.UiPaginationView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostComposerMediaPickerView {

    public static final String ATTRIBUTE = "picker";

    private final String id;
    private final String searchPath;
    private final String refreshPath;
    private final String originalName;
    private final List<PostComposerMediaItemView> items;
    private final UiPaginationView pagination;
}
