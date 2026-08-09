package com.app.features.post.shortpost.schema.result;

import com.app.features.post.schema.result.PostMediaResult;
import com.app.features.post.schema.result.PostSummaryResult;

import lombok.Data;

@Data
public class PublicShortPostResult {

    private PostSummaryResult post;

    private String caption;

    private PostMediaResult media;
}
