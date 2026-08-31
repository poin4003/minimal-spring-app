package com.app.features.ai.rag.schema.payload;

import com.app.features.ai.rag.enums.PostRagMessageRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostRagConversationMessagePayload {

    @NotNull(message = "{validation.ai.rag.history.role.required}")
    private PostRagMessageRole role;

    @NotBlank(message = "{validation.ai.rag.history.content.required}")
    @Size(
            max = 8000,
            message = "{validation.ai.rag.history.content.max}")
    private String content;
}
