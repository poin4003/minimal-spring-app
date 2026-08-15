package com.app.features.post.moderation.schema.result;

import com.app.features.post.schema.result.PostSummaryResult;

import lombok.Data;

@Data
public class ModerationVideoPostDetailResult
        implements ModerationPostDetailResult {

    private PostSummaryResult post;

    private ModerationPostStateResult state;

    private String title;

    private String description;

    private ModerationPostMediaResult content;
}
