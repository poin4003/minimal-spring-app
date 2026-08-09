package com.app.features.post.standard.web.view;

import com.app.features.post.standard.schema.result.PublicStandardPostResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicPostCardView {

    private final PublicStandardPostResult post;
    private final String detailPath;
}
