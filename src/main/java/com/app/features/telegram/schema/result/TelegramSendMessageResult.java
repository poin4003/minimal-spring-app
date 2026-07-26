package com.app.features.telegram.schema.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramSendMessageResult(
        @JsonProperty("message_id") Long messageId) {
}
