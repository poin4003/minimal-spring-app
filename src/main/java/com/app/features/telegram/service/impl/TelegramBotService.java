package com.app.features.telegram.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.app.config.settings.AppProperties;
import com.app.core.exception.ExceptionFactory;
import com.app.features.telegram.schema.model.TelegramSendMessageRequest;
import com.app.features.telegram.schema.payload.TelegramPayload;
import com.app.features.telegram.schema.result.TelegramSendMessageResponse;
import com.app.features.telegram.service.TelegramService;

@Service
@Validated
@ConditionalOnProperty(
        prefix = "app.notification.telegram",
        name = "enabled",
        havingValue = "true")
public class TelegramBotService implements TelegramService {

    private final RestClient restClient;
    private final AppProperties appProperties;

    public TelegramBotService(
            RestClient.Builder restClientBuilder,
            AppProperties appProperties) {
        this.appProperties = appProperties;
        this.restClient = restClientBuilder
                .baseUrl(
                        appProperties.getNotification()
                                .getTelegram()
                                .getApiBaseUrl())
                .build();
    }

    @Override
    public String send(TelegramPayload payload) {
        TelegramSendMessageRequest request =
                new TelegramSendMessageRequest(
                        payload.getChatId(),
                        payload.getContent());

        try {
            TelegramSendMessageResponse response = restClient.post()
                    .uri(
                            "/bot{token}/sendMessage",
                            appProperties.getNotification()
                                    .getTelegram()
                                    .getBotToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TelegramSendMessageResponse.class);

            if (response == null
                    || !response.ok()
                    || response.result() == null
                    || response.result().messageId() == null) {
                throw ExceptionFactory.serverError(
                        "error.telegram.invalidResponse");
            }

            return response.result().messageId().toString();
        } catch (RestClientResponseException exception) {
            throw ExceptionFactory.serverError(
                    "error.telegram.httpResponse",
                    exception,
                    exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw ExceptionFactory.serverError(
                    "error.telegram.unreachable",
                    exception);
        }
    }
}
