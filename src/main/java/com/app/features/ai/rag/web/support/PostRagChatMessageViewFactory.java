package com.app.features.ai.rag.web.support;

import org.springframework.stereotype.Component;
import com.app.core.i18n.AppMessageResolver;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.rag.schema.model.PostRagResult;
import com.app.features.ai.rag.web.view.PostRagChatMessageView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostRagChatMessageViewFactory {

    private final AppMessageResolver messageResolver;
    private final PostRagChatSourceViewFactory chatSourceViewFactory;

    public PostRagChatMessageView build(PostRagResult result) {
        return PostRagChatMessageView.builder()
                .generated(result.hasGeneratedAnswer())
                .answer(resolveAnswer(result))
                .sources(result.sources().stream()
                        .map(source -> chatSourceViewFactory.build(source))
                        .toList())
                .build();
    }

    private String resolveAnswer(PostRagResult result) {
        if (result.hasGeneratedAnswer()) {
            return result.generatedAnswer().text();
        }
        if (result.retrievalAvailability() != AiAvailability.READY) {
            return messageResolver.get(
                    "ai.chat.response.retrievalUnavailable");
        }
        if (result.sources().isEmpty()) {
            return messageResolver.get("ai.chat.response.noSources");
        }
        return messageResolver.get("ai.chat.response.retrievalOnly");
    }

}
