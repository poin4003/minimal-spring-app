package com.app.features.ai.rag.schema.model;

import java.util.List;
import java.util.Objects;

import com.app.core.enums.AppLanguage;

public record PostRagConversationRequest(
        String question,
        List<PostRagConversationMessage> history,
        AppLanguage responseLanguage) {

    public PostRagConversationRequest {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "question must not be blank");
        }
        question = question.trim();
        history = List.copyOf(Objects.requireNonNull(
                history,
                "history must not be null"));
        responseLanguage = Objects.requireNonNull(
                responseLanguage,
                "responseLanguage must not be null");
    }

    public static PostRagConversationRequest withoutHistory(
            String question) {
        return withoutHistory(question, AppLanguage.EN);
    }

    public static PostRagConversationRequest withoutHistory(
            String question,
            AppLanguage responseLanguage) {
        return new PostRagConversationRequest(
                question,
                List.of(),
                responseLanguage);
    }
}
