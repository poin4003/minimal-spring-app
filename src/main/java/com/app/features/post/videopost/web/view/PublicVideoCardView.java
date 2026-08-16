package com.app.features.post.videopost.web.view;

import com.app.features.post.videopost.schema.result.PublicVideoPostResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicVideoCardView {

    private final PublicVideoPostResult video;
    private final String detailPath;
}
