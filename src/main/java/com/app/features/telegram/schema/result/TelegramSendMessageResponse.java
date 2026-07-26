package com.app.features.telegram.schema.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramSendMessageResponse(
        boolean ok,
        TelegramSendMessageResult result,
        String description) {
}
