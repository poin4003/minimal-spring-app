package com.app.features.post.aimoderation.integration.jlama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import com.app.config.settings.AppProperties;
import com.app.features.ai.generation.schema.model.AiTextGenerationRequest;
import com.app.features.ai.generation.schema.model.AiTextGenerationResult;
import com.app.features.ai.generation.service.AiTextGenerationClient;
import com.app.features.post.aimoderation.enums.PostAiModerationOutcome;
import com.app.features.post.aimoderation.exceptions.PostAiModerationClientException;
import com.app.features.post.aimoderation.schema.model.PostAiModerationClientResult;
import com.app.features.post.aimoderation.schema.model.PostAiModerationRequest;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class JlamaPostAiModerationClientTests {

    @Mock
    private ObjectProvider<AiTextGenerationClient>
            aiTextGenerationClientProvider;

    @Mock
    private AiTextGenerationClient aiTextGenerationClient;

    private JlamaPostAiModerationClient client;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getPost()
                .getAiModeration()
                .getMachine()
                .setMaxTokens(64);
        client = new JlamaPostAiModerationClient(
                aiTextGenerationClientProvider,
                appProperties,
                new ObjectMapper());

        given(aiTextGenerationClientProvider.getIfAvailable())
                .willReturn(aiTextGenerationClient);
        given(aiTextGenerationClient.isReady()).willReturn(true);
    }

    @Test
    void placesOutputFormatAfterUntrustedUserContent() {
        given(aiTextGenerationClient.getModelId())
                .willReturn("test/model");
        given(aiTextGenerationClient.generate(
                any(AiTextGenerationRequest.class)))
                .willReturn(generation("""
                        {"outcome":"REJECT","reason":"Racial attack"}
                        """));

        PostAiModerationClientResult result = client.moderate(request());

        assertThat(result.outcome())
                .isEqualTo(PostAiModerationOutcome.REJECT);

        ArgumentCaptor<AiTextGenerationRequest> generationRequest =
                ArgumentCaptor.forClass(AiTextGenerationRequest.class);
        verify(aiTextGenerationClient).generate(
                generationRequest.capture());

        assertThat(generationRequest.getValue().systemPrompt())
                .contains("This runtime is text-only")
                .doesNotContain("Return exactly one JSON object");
        assertThat(generationRequest.getValue().userPrompt())
                .startsWith("Post text:")
                .contains("Return exactly one JSON object")
                .endsWith("Do not include markdown, code fences, or any text outside the JSON.\n");
        assertThat(generationRequest.getValue().temperature())
                .isEqualTo(0.0f);
        assertThat(generationRequest.getValue().maxOutputTokens())
                .isEqualTo(64);
    }

    @Test
    void preservesRawResponseWhenModelDoesNotReturnJson() {
        String rawResponse = "REJECT because the post contains a racial slur.";
        given(aiTextGenerationClient.generate(
                any(AiTextGenerationRequest.class)))
                .willReturn(generation(rawResponse));

        assertThatExceptionOfType(PostAiModerationClientException.class)
                .isThrownBy(() -> client.moderate(request()))
                .satisfies(exception -> assertThat(exception.getRawResponse())
                        .isEqualTo(rawResponse));
    }

    private AiTextGenerationResult generation(String responseText) {
        return new AiTextGenerationResult(
                responseText,
                10,
                5,
                1,
                2,
                "STOP");
    }

    private PostAiModerationRequest request() {
        return new PostAiModerationRequest(
                "Moderation policy",
                "Post text:\nUntrusted content",
                List.of());
    }
}
