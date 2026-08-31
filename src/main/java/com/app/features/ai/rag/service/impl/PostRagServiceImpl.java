package com.app.features.ai.rag.service.impl;

import java.util.concurrent.CancellationException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.generation.schema.model.AiTextGenerationRequest;
import com.app.features.ai.generation.schema.model.AiTextGenerationResult;
import com.app.features.ai.generation.service.AiTextGenerationClient;
import com.app.features.ai.generation.service.AiTextTokenCounter;
import com.app.features.ai.rag.schema.model.PostRagConversationRequest;
import com.app.features.ai.rag.schema.model.PostRagContext;
import com.app.features.ai.rag.schema.model.PostRagGeneratedAnswer;
import com.app.features.ai.rag.schema.model.PostRagPrompt;
import com.app.features.ai.rag.schema.model.PostRagResult;
import com.app.features.ai.rag.schema.model.PostRagStreamMetadata;
import com.app.features.ai.rag.service.PostRagService;
import com.app.features.ai.rag.service.PostRagStreamObserver;
import com.app.features.ai.rag.support.PostRagConversationWindowFactory;
import com.app.features.ai.rag.support.PostRagContextFactory;
import com.app.features.ai.rag.support.PostRagPromptBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class PostRagServiceImpl implements PostRagService {

    private final AppProperties appProperties;
    private final PostRagContextFactory postRagContextFactory;
    private final PostRagPromptBuilder postRagPromptBuilder;
    private final PostRagConversationWindowFactory
            postRagConversationWindowFactory;
    private final ObjectProvider<AiTextGenerationClient>
            aiTextGenerationClientProvider;
    private final ObjectProvider<AiTextTokenCounter>
            aiTextTokenCounterProvider;

    @Override
    public PostRagResult answer(String question) {
        return answer(
                PostRagConversationRequest.withoutHistory(question),
                NoOpPostRagStreamObserver.INSTANCE);
    }

    @Override
    public PostRagResult answer(
            String question,
            PostRagStreamObserver streamObserver) {
        return answer(
                PostRagConversationRequest.withoutHistory(question),
                streamObserver);
    }

    @Override
    public PostRagResult answer(PostRagConversationRequest request) {
        return answer(request, NoOpPostRagStreamObserver.INSTANCE);
    }

    @Override
    public PostRagResult answer(
            PostRagConversationRequest request,
            PostRagStreamObserver streamObserver) {
        ensureActive(streamObserver);
        GenerationRuntime generationRuntime = resolveGenerationRuntime(
                !request.history().isEmpty());
        PostRagConversationRequest conversation =
                postRagConversationWindowFactory.create(
                        request,
                        generationRuntime.tokenCounter());
        PostRagContext context = postRagContextFactory.create(conversation);
        streamObserver.onMetadata(new PostRagStreamMetadata(
                context.retrievalAvailability(),
                generationRuntime.availability(),
                context.sources()));
        ensureActive(streamObserver);

        if (context.retrievalAvailability() != AiAvailability.READY
                || context.sources().isEmpty()
                || generationRuntime.availability()
                        != AiAvailability.READY) {
            return retrievalOnly(context, generationRuntime.availability());
        }

        PostRagPrompt prompt = postRagPromptBuilder.build(
                context,
                conversation.history(),
                conversation.responseLanguage());
        try {
            AiTextGenerationResult generation = generationRuntime.client()
                    .generate(new AiTextGenerationRequest(
                            prompt.systemPrompt(),
                            prompt.userPrompt(),
                            appProperties.getAi().getRag()
                                    .getTemperature(),
                            appProperties.getAi().getRag()
                                    .getMaxOutputTokens()),
                            new PostRagAiTextGenerationStreamObserver(
                                    streamObserver));
            if (!StringUtils.hasText(generation.responseText())) {
                log.warn(
                        "RAG generation returned an empty response; "
                                + "falling back to retrieved sources.");
                return retrievalOnly(
                        context,
                        AiAvailability.UNAVAILABLE);
            }

            PostRagGeneratedAnswer generatedAnswer =
                    new PostRagGeneratedAnswer(
                            generation.responseText(),
                            generationRuntime.client().getModelId(),
                            generation.promptTokens(),
                            generation.generatedTokens(),
                            generation.promptTimeMs(),
                            generation.generationTimeMs(),
                            generation.finishReason());
            return new PostRagResult(
                    context.question(),
                    context.retrievalAvailability(),
                    AiAvailability.READY,
                    prompt.sources(),
                    generatedAnswer);
        } catch (CancellationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn(
                    "RAG generation failed; falling back to retrieved sources.",
                    exception);
            return retrievalOnly(context, AiAvailability.UNAVAILABLE);
        }
    }

    private void ensureActive(PostRagStreamObserver streamObserver) {
        if (streamObserver.isCancelled()) {
            throw new CancellationException(
                    "RAG stream was cancelled by the client.");
        }
    }

    private GenerationRuntime resolveGenerationRuntime(
            boolean tokenCounterRequired) {
        if (!appProperties.getAi().getGeneration().isEnabled()) {
            return new GenerationRuntime(
                    AiAvailability.DISABLED,
                    null,
                    null);
        }

        AiTextGenerationClient generationClient =
                aiTextGenerationClientProvider.getIfAvailable();
        if (generationClient == null || !generationClient.isReady()) {
            return new GenerationRuntime(
                    AiAvailability.UNAVAILABLE,
                    null,
                    null);
        }

        AiTextTokenCounter tokenCounter =
                aiTextTokenCounterProvider.getIfAvailable();
        if (tokenCounterRequired
                && (tokenCounter == null || !tokenCounter.isReady())) {
            return new GenerationRuntime(
                    AiAvailability.UNAVAILABLE,
                    null,
                    null);
        }

        return new GenerationRuntime(
                AiAvailability.READY,
                generationClient,
                tokenCounter);
    }

    private PostRagResult retrievalOnly(
            PostRagContext context,
            AiAvailability generationAvailability) {
        return new PostRagResult(
                context.question(),
                context.retrievalAvailability(),
                generationAvailability,
                context.sources(),
                null);
    }

    private record GenerationRuntime(
            AiAvailability availability,
            AiTextGenerationClient client,
            AiTextTokenCounter tokenCounter) {
    }
}
