package com.app.features.post.moderation.web.view;

import java.util.List;

import com.app.features.post.moderation.schema.result.ModerationPostMediaResult;
import com.app.features.post.moderation.schema.result.ModerationPostStateResult;
import com.app.features.post.schema.result.PostSummaryResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModerationPostDetailView {

    private final PostSummaryResult post;
    private final ModerationPostStateResult state;
    private final String content;

    @Builder.Default
    private final List<ModerationPostMediaResult> media = List.of();
}
