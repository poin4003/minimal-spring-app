package com.app.features.post.shortpost.web.view;

import com.app.features.post.shortpost.schema.result.PublicShortPostResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicShortCardView {

    private final PublicShortPostResult post;
    private final String detailPath;
}
