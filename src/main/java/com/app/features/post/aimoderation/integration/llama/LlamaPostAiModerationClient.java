package com.app.features.post.aimoderation.integration.llama;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.app.config.settings.AppProperties;
import com.app.features.post.aimoderation.exceptions.PostAiModerationClientException;
import com.app.features.post.aimoderation.integration.llama.schema.model.LlamaChatChoice;
import com.app.features.post.aimoderation.integration.llama.schema.model.LlamaChatCompletionRequest;
import com.app.features.post.aimoderation.integration.llama.schema.model.LlamaChatCompletionResponse;
import com.app.features.post.aimoderation.integration.llama.schema.model.LlamaChatContentItem;
import com.app.features.post.aimoderation.integration.llama.schema.model.LlamaChatMessage;
import com.app.features.post.aimoderation.integration.llama.schema.model.LlamaResponseFormat;
import com.app.features.post.aimoderation.schema.model.PostAiModerationClientResult;
import com.app.features.post.aimoderation.schema.model.PostAiModerationRequest;
import com.app.features.post.aimoderation.schema.model.PostAiModerationStructuredOutput;
import com.app.features.post.aimoderation.service.PostAiModerationClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Validated
public class LlamaPostAiModerationClient implements PostAiModerationClient {

    private static final double TEMPERATURE = 0.0d;

    private static final String OUTPUT_SCHEMA_JSON = """
            {
              "type": "object",
              "additionalProperties": false,
              "required": ["outcome", "reason"],
              "properties": {
                "outcome": {
                  "type": "string",
                  "enum": ["APPROVE", "REJECT", "ESCALATE"]
                },
                "reason": {
                  "type": "string",
                  "minLength": 1
                }
              }
            }
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final JsonNode responseSchema;

    public LlamaPostAiModerationClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.responseSchema = parseResponseSchema();

        int timeoutMillis = Math.toIntExact(
                appProperties.getPost()
                        .getAiModeration()
                        .getMachine()
                        .getTimeout()
                        .toMillis());

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);

        this.restClient = restClientBuilder
                .baseUrl(appProperties.getPost()
                        .getAiModeration()
                        .getMachine()
                        .getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public PostAiModerationClientResult moderate(PostAiModerationRequest request) {
        LlamaChatCompletionRequest llamaRequest = buildRequest(request);

        try {
            LlamaChatCompletionResponse response = restClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(llamaRequest)
                    .retrieve()
                    .body(LlamaChatCompletionResponse.class);

            String rawContent = extractAssistantContent(response);
            PostAiModerationStructuredOutput output = parseStructuredOutput(rawContent);

            return new PostAiModerationClientResult(
                    output.outcome(),
                    normalize(output.reason()),
                    rawContent,
                    resolveModelName(response));
        } catch (RestClientResponseException exception) {
            throw new PostAiModerationClientException(
                    "llama-server returned HTTP " + exception.getStatusCode().value(),
                    exception);
        } catch (RestClientException exception) {
            throw new PostAiModerationClientException(
                    "llama-server is unreachable.",
                    exception);
        }
    }

    private JsonNode parseResponseSchema() {
        try {
            return objectMapper.readTree(OUTPUT_SCHEMA_JSON);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Unable to initialize AI moderation response schema.",
                    exception);
        }
    }

    private LlamaChatCompletionRequest buildRequest(PostAiModerationRequest request) {
        List<LlamaChatContentItem> userContent = new ArrayList<>();
        userContent.add(LlamaChatContentItem.text(request.userPrompt()));

        request.imageUrls().stream()
                .limit(appProperties.getPost()
                        .getAiModeration()
                        .getMachine()
                        .getMaxImages())
                .forEach(imageUrl -> userContent.add(
                        LlamaChatContentItem.image(imageUrl)));

        return new LlamaChatCompletionRequest(
                appProperties.getPost()
                        .getAiModeration()
                        .getMachine()
                        .getModel(),
                List.of(
                        new LlamaChatMessage(
                                "system",
                                List.of(LlamaChatContentItem.text(
                                        request.systemPrompt()))),
                        new LlamaChatMessage("user", userContent)),
                TEMPERATURE,
                appProperties.getPost()
                        .getAiModeration()
                        .getMachine()
                        .getMaxTokens(),
                new LlamaResponseFormat("json_object", responseSchema));
    }

    private String extractAssistantContent(LlamaChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new PostAiModerationClientException(
                    "llama-server returned an empty moderation response.");
        }

        LlamaChatChoice choice = response.choices().getFirst();
        if (choice.message() == null || !StringUtils.hasText(choice.message().content())) {
            throw new PostAiModerationClientException(
                    "llama-server returned an empty moderation response.");
        }

        return choice.message().content().trim();
    }

    private PostAiModerationStructuredOutput parseStructuredOutput(String rawContent) {
        try {
            PostAiModerationStructuredOutput output = objectMapper.readValue(
                    rawContent,
                    PostAiModerationStructuredOutput.class);
            if (output == null || output.outcome() == null) {
                throw new PostAiModerationClientException(
                        "llama-server returned moderation JSON without outcome.");
            }

            String normalizedReason = normalize(output.reason());
            if (!StringUtils.hasText(normalizedReason)) {
                throw new PostAiModerationClientException(
                        "llama-server returned moderation JSON without reason.");
            }

            return new PostAiModerationStructuredOutput(
                    output.outcome(),
                    normalizedReason);
        } catch (RuntimeException exception) {
            throw new PostAiModerationClientException(
                    "Unable to parse moderation JSON returned by llama-server.",
                    exception);
        }
    }

    private String resolveModelName(LlamaChatCompletionResponse response) {
        if (response != null && StringUtils.hasText(response.model())) {
            return response.model().trim();
        }

        return appProperties.getPost()
                .getAiModeration()
                .getMachine()
                .getModel();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
