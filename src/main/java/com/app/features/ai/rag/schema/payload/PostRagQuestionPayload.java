package com.app.features.ai.rag.schema.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostRagQuestionPayload {

    @NotBlank(message = "{validation.ai.rag.question.required}")
    @Size(
            max = 2000,
            message = "{validation.ai.rag.question.max}")
    private String question;
}
