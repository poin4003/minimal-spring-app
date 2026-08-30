package com.app.features.ai.rag.service.impl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.generation.schema.model.AiTextGenerationRequest;
import com.app.features.ai.generation.schema.model.AiTextGenerationResult;
import com.app.features.ai.generation.service.AiTextGenerationClient;
import com.app.features.ai.rag.schema.model.PostRagContext;
import com.app.features.ai.rag.schema.model.PostRagGeneratedAnswer;
import com.app.features.ai.rag.schema.model.PostRagPrompt;
import com.app.features.ai.rag.schema.model.PostRagResult;
import com.app.features.ai.rag.service.PostRagService;
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
    private final ObjectProvider<AiTextGenerationClient>
            aiTextGenerationClientProvider;

    @Override
    public PostRagResult answer(String question) {
        PostRagContext context = postRagContextFactory.create(question);
        GenerationRuntime generationRuntime = resolveGenerationRuntime();

        if (context.retrievalAvailability() != AiAvailability.READY
                || context.sources().isEmpty()
                || generationRuntime.availability()
                        != AiAvailability.READY) {
            return retrievalOnly(context, generationRuntime.availability());
        }

        PostRagPrompt prompt = postRagPromptBuilder.build(context);
        try {
            AiTextGenerationResult generation = generationRuntime.client()
                    .generate(new AiTextGenerationRequest(
                            prompt.systemPrompt(),
                            prompt.userPrompt(),
                            appProperties.getAi().getRag()
                                    .getTemperature(),
                            appProperties.getAi().getRag()
                                    .getMaxOutputTokens()));
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
        } catch (RuntimeException exception) {
            log.warn(
                    "RAG generation failed; falling back to retrieved sources.",
                    exception);
            return retrievalOnly(context, AiAvailability.UNAVAILABLE);
        }
    }

    private GenerationRuntime resolveGenerationRuntime() {
        if (!appProperties.getAi().getGeneration().isEnabled()) {
            return new GenerationRuntime(
                    AiAvailability.DISABLED,
                    null);
        }

        AiTextGenerationClient generationClient =
                aiTextGenerationClientProvider.getIfAvailable();
        if (generationClient == null || !generationClient.isReady()) {
            return new GenerationRuntime(
                    AiAvailability.UNAVAILABLE,
                    null);
        }

        return new GenerationRuntime(
                AiAvailability.READY,
                generationClient);
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
            AiTextGenerationClient client) {
    }
}
