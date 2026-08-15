package com.app.features.post.videopost.schema.result;

import com.app.features.post.schema.result.PostMediaResult;
import com.app.features.post.schema.result.PostSummaryResult;

import lombok.Data;

@Data
public class PublicVideoPostResult {

    private PostSummaryResult post;

    private String title;

    private String description;

    private PostMediaResult content;
}
