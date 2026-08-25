package com.app.features.post.aimoderation.integration.jlama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.app.config.settings.AppProperties;
import com.app.features.ai.runtime.JlamaRuntime;
import com.app.features.post.aimoderation.enums.PostAiModerationOutcome;
import com.app.features.post.aimoderation.exceptions.PostAiModerationClientException;
import com.app.features.post.aimoderation.schema.model.PostAiModerationClientResult;
import com.app.features.post.aimoderation.schema.model.PostAiModerationRequest;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class JlamaPostAiModerationClientTests {

    @Mock
    private JlamaRuntime jlamaRuntime;

    private JlamaPostAiModerationClient client;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getPost()
                .getAiModeration()
                .getMachine()
                .setMaxTokens(64);
        client = new JlamaPostAiModerationClient(
                jlamaRuntime,
                appProperties,
                new ObjectMapper());

        given(jlamaRuntime.isReady()).willReturn(true);
    }

    @Test
    void placesOutputFormatAfterUntrustedUserContent() {
        given(jlamaRuntime.generate(
                anyString(),
                anyString(),
                eq(0.0f),
                eq(64)))
                .willReturn("""
                        {"outcome":"REJECT","reason":"Racial attack"}
                        """);

        PostAiModerationClientResult result = client.moderate(request());

        assertThat(result.outcome())
                .isEqualTo(PostAiModerationOutcome.REJECT);

        ArgumentCaptor<String> systemPrompt =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPrompt =
                ArgumentCaptor.forClass(String.class);
        verify(jlamaRuntime).generate(
                systemPrompt.capture(),
                userPrompt.capture(),
                eq(0.0f),
                eq(64));

        assertThat(systemPrompt.getValue())
                .contains("This runtime is text-only")
                .doesNotContain("Return exactly one JSON object");
        assertThat(userPrompt.getValue())
                .startsWith("Post text:")
                .contains("Return exactly one JSON object")
                .endsWith("Do not include markdown, code fences, or any text outside the JSON.\n");
    }

    @Test
    void preservesRawResponseWhenModelDoesNotReturnJson() {
        String rawResponse = "REJECT because the post contains a racial slur.";
        given(jlamaRuntime.generate(
                anyString(),
                anyString(),
                eq(0.0f),
                eq(64)))
                .willReturn(rawResponse);

        assertThatExceptionOfType(PostAiModerationClientException.class)
                .isThrownBy(() -> client.moderate(request()))
                .satisfies(exception -> assertThat(exception.getRawResponse())
                        .isEqualTo(rawResponse));
    }

    private PostAiModerationRequest request() {
        return new PostAiModerationRequest(
                "Moderation policy",
                "Post text:\nUntrusted content",
                List.of());
    }
}
