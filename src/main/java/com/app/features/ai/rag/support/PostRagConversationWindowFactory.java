package com.app.features.ai.rag.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.app.config.settings.AppProperties;
import com.app.features.ai.generation.service.AiTextTokenCounter;
import com.app.features.ai.rag.schema.model.PostRagConversationMessage;
import com.app.features.ai.rag.schema.model.PostRagConversationRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostRagConversationWindowFactory {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public PostRagConversationRequest create(
            PostRagConversationRequest request,
            AiTextTokenCounter tokenCounter) {
        List<PostRagConversationMessage> messageWindow =
                limitMessageCount(request.history());
        if (messageWindow.isEmpty()
                || tokenCounter == null
                || !tokenCounter.isReady()) {
            return new PostRagConversationRequest(
                    request.question(),
                    messageWindow,
                    request.responseLanguage());
        }

        try {
            return new PostRagConversationRequest(
                    request.question(),
                    limitTokenCount(messageWindow, tokenCounter),
                    request.responseLanguage());
        } catch (RuntimeException exception) {
            log.warn(
                    "Unable to build the RAG conversation window; "
                            + "continuing without history.",
                    exception);
            return new PostRagConversationRequest(
                    request.question(),
                    List.of(),
                    request.responseLanguage());
        }
    }

    private List<PostRagConversationMessage> limitMessageCount(
            List<PostRagConversationMessage> history) {
        int maxMessages = appProperties.getAi()
                .getRag()
                .getConversation()
                .getMaxHistoryMessages();
        int firstIncludedIndex = Math.max(
                0,
                history.size() - maxMessages);
        return List.copyOf(history.subList(
                firstIncludedIndex,
                history.size()));
    }

    private List<PostRagConversationMessage> limitTokenCount(
            List<PostRagConversationMessage> history,
            AiTextTokenCounter tokenCounter) {
        int maxTokens = appProperties.getAi()
                .getRag()
                .getConversation()
                .getMaxHistoryTokens();
        List<PostRagConversationMessage> selected = new ArrayList<>();

        for (int index = history.size() - 1; index >= 0; index--) {
            PostRagConversationMessage message = history.get(index);
            selected.add(0, message);
            if (countTokens(selected, tokenCounter) <= maxTokens) {
                continue;
            }

            selected.remove(0);
            if (selected.isEmpty()) {
                PostRagConversationMessage truncated = truncateToFit(
                        message,
                        maxTokens,
                        tokenCounter);
                if (truncated != null) {
                    selected.add(truncated);
                }
            }
            break;
        }

        return List.copyOf(selected);
    }

    private PostRagConversationMessage truncateToFit(
            PostRagConversationMessage message,
            int maxTokens,
            AiTextTokenCounter tokenCounter) {
        int lowerBound = 1;
        int upperBound = message.content().length();
        PostRagConversationMessage bestMatch = null;

        while (lowerBound <= upperBound) {
            int candidateLength = lowerBound
                    + (upperBound - lowerBound) / 2;
            PostRagConversationMessage candidate =
                    new PostRagConversationMessage(
                            message.role(),
                            message.content().substring(0, candidateLength));
            if (countTokens(List.of(candidate), tokenCounter) <= maxTokens) {
                bestMatch = candidate;
                lowerBound = candidateLength + 1;
            } else {
                upperBound = candidateLength - 1;
            }
        }

        return bestMatch;
    }

    private int countTokens(
            List<PostRagConversationMessage> messages,
            AiTextTokenCounter tokenCounter) {
        String serializedHistory = objectMapper.writeValueAsString(messages);
        return tokenCounter.countTokens(serializedHistory);
    }
}
