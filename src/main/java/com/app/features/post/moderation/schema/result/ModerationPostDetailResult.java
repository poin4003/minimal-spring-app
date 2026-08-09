package com.app.features.post.moderation.schema.result;

import com.app.features.post.schema.result.PostSummaryResult;

public interface ModerationPostDetailResult {

    PostSummaryResult getPost();

    ModerationPostStateResult getState();
}
