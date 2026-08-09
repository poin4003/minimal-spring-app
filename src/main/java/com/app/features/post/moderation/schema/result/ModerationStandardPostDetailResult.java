package com.app.features.post.moderation.schema.result;

import java.util.List;
import com.app.features.post.schema.result.PostSummaryResult;

import lombok.Data;

@Data
public class ModerationStandardPostDetailResult
        implements ModerationPostDetailResult {

    private PostSummaryResult post;

    private ModerationPostStateResult state;

    private String content;

    private List<ModerationPostMediaResult> media = List.of();
}
