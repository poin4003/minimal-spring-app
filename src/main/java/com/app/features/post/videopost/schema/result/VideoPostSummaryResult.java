package com.app.features.post.videopost.schema.result;

import com.app.features.post.schema.result.PostMediaResult;
import com.app.features.post.schema.result.PostSummaryResult;

import lombok.Data;

@Data
public class VideoPostSummaryResult {

    private PostSummaryResult post;

    private String title;

    private PostMediaResult source;
}
