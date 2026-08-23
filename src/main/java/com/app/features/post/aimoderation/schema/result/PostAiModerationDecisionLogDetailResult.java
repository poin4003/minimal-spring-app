package com.app.features.post.aimoderation.schema.result;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostAiModerationDecisionLogDetailResult
        extends PostAiModerationDecisionLogResult {

    private String promptSnapshot;

    private String rawResponse;
}
