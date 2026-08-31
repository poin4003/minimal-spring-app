package com.app.features.ai.search.web.view;

import java.util.List;

import com.app.features.post.enums.PostType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostSearchSectionView {

    private final PostType postType;
    private final String title;
    private final List<PostSearchItemView> items;
}
