package com.app.features.post.standard.web.view;

import com.app.features.post.schema.result.PostMediaResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostDetailMediaItemView {

    private final PostMediaResult attachment;
    private final String galleryPartialPath;
}
