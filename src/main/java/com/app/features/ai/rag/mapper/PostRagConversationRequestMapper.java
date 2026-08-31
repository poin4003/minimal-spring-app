package com.app.features.ai.rag.mapper;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.app.features.ai.rag.schema.model.PostRagConversationMessage;
import com.app.features.ai.rag.schema.model.PostRagConversationRequest;
import com.app.features.ai.rag.schema.payload.PostRagQuestionPayload;
import com.app.features.ai.rag.support.PostRagLanguageResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostRagConversationRequestMapper {

    private final PostRagLanguageResolver postRagLanguageResolver;

    public PostRagConversationRequest toModel(
            PostRagQuestionPayload payload,
            Locale locale) {
        return new PostRagConversationRequest(
                payload.getQuestion(),
                payload.getHistory().stream()
                        .map(message -> new PostRagConversationMessage(
                                message.getRole(),
                                message.getContent()))
                        .toList(),
                postRagLanguageResolver.resolve(locale));
    }
}
