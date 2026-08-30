package com.app.features.post.aimoderation.integration.jlama;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.app.config.settings.AppProperties;
import com.app.features.ai.generation.schema.model.AiTextGenerationRequest;
import com.app.features.ai.generation.schema.model.AiTextGenerationResult;
import com.app.features.ai.generation.service.AiTextGenerationClient;
import com.app.features.post.aimoderation.enums.PostAiModerationOutcome;
import com.app.features.post.aimoderation.exceptions.PostAiModerationClientException;
import com.app.features.post.aimoderation.schema.model.PostAiModerationClientResult;
import com.app.features.post.aimoderation.schema.model.PostAiModerationRequest;
import com.app.features.post.aimoderation.schema.model.PostAiModerationStructuredOutput;
import com.app.features.post.aimoderation.service.PostAiModerationClient;
import com.app.features.post.aimoderation.service.PostAiModerationHealthClient;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.post.ai-moderation",
        name = "enabled",
        havingValue = "true")
public class JlamaPostAiModerationClient
        implements PostAiModerationClient, PostAiModerationHealthClient {

    private static final float TEMPERATURE = 0.0f;
    private static final String RUNTIME_INSTRUCTIONS = """

            This runtime is text-only. Thumbnail URLs are references only and
            their visual content has not been inspected. Never claim that you
            inspected an image. If the policy requires media inspection and
            the text alone is insufficient, choose ESCALATE.
            """;
    private static final String OUTPUT_INSTRUCTIONS = """

            Respond to the moderation request above now.
            Return exactly one JSON object using this schema:
            {"outcome":"APPROVE|REJECT|ESCALATE","reason":"maximum 12 words"}
            Do not include markdown, code fences, or any text outside the JSON.
            """;

    private final ObjectProvider<AiTextGenerationClient>
            aiTextGenerationClientProvider;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Override
    public PostAiModerationClientResult moderate(
            PostAiModerationRequest request) {
        AiTextGenerationClient generationClient =
                aiTextGenerationClientProvider.getIfAvailable();
        if (generationClient == null || !generationClient.isReady()) {
            throw new PostAiModerationClientException(
                    "Jlama moderation runtime is unavailable.");
        }

        try {
            AiTextGenerationResult generation = generationClient.generate(
                    new AiTextGenerationRequest(
                            request.systemPrompt() + RUNTIME_INSTRUCTIONS,
                            request.userPrompt() + OUTPUT_INSTRUCTIONS,
                            TEMPERATURE,
                            appProperties.getPost()
                                    .getAiModeration()
                                    .getMachine()
                                    .getMaxTokens()));
            String rawResponse = generation.responseText();
            PostAiModerationStructuredOutput output = parse(rawResponse);

            return new PostAiModerationClientResult(
                    output.outcome(),
                    output.reason().trim(),
                    rawResponse,
                    generationClient.getModelId());
        } catch (PostAiModerationClientException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PostAiModerationClientException(
                    "Jlama moderation inference failed.",
                    exception);
        }
    }

    @Override
    public boolean isReady() {
        AiTextGenerationClient generationClient =
                aiTextGenerationClientProvider.getIfAvailable();
        return generationClient != null && generationClient.isReady();
    }

    private PostAiModerationStructuredOutput parse(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            throw new PostAiModerationClientException(
                    "Jlama returned an empty moderation response.",
                    rawResponse);
        }

        int jsonStart = rawResponse.indexOf('{');
        int jsonEnd = rawResponse.lastIndexOf('}');
        if (jsonStart < 0 || jsonEnd <= jsonStart) {
            throw new PostAiModerationClientException(
                    "Jlama moderation response does not contain a JSON object.",
                    rawResponse);
        }

        try {
            PostAiModerationStructuredOutput output = objectMapper.readValue(
                    rawResponse.substring(jsonStart, jsonEnd + 1),
                    PostAiModerationStructuredOutput.class);

            if (output.outcome() == null
                    || output.outcome() == PostAiModerationOutcome.ERROR
                    || !StringUtils.hasText(output.reason())) {
                throw new PostAiModerationClientException(
                        "Jlama moderation response contains invalid fields.",
                        rawResponse);
            }

            return output;
        } catch (PostAiModerationClientException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PostAiModerationClientException(
                    "Unable to parse Jlama moderation JSON.",
                    rawResponse,
                    exception);
        }
    }
}
