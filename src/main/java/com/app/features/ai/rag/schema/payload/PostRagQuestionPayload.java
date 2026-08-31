package com.app.features.ai.rag.schema.payload;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostRagQuestionPayload {

    @NotBlank(message = "{validation.ai.rag.question.required}")
    @Size(
            max = 2000,
            message = "{validation.ai.rag.question.max}")
    private String question;

    @Valid
    @NotNull(message = "{validation.ai.rag.history.required}")
    private List<PostRagConversationMessagePayload> history =
            new ArrayList<>();
}
