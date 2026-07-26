package com.app.features.telegram.schema.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramSendMessageRequest(
        @JsonProperty("chat_id") String chatId,
        String text) {
}
