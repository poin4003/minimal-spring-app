package com.app.features.telegram.schema.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TelegramPayload {

    @NotBlank
    @Size(max = 255)
    private String chatId;

    @NotBlank
    @Size(max = 4096)
    private String content;
}
