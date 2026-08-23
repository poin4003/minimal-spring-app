package com.app.features.post.aimoderation.web.view;

import com.app.features.post.aimoderation.enums.PostAiModerationMode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostAiModerationConfigForm {

    @NotNull(message = "{validation.post.aiModeration.mode.required}")
    private PostAiModerationMode mode;

    @Size(
            max = 10_000,
            message = "{validation.post.aiModeration.prompt.tooLong}")
    private String promptText;
}
