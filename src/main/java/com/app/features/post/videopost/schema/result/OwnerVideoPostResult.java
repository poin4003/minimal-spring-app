package com.app.features.post.videopost.schema.result;

import com.app.features.post.schema.result.OwnerPostStateResult;
import com.app.features.post.schema.result.PostMediaResult;
import com.app.features.post.schema.result.PostSummaryResult;

import lombok.Data;

@Data
public class OwnerVideoPostResult {

    private PostSummaryResult post;

    private OwnerPostStateResult state;

    private String title;

    private String description;

    private PostMediaResult source;
}
