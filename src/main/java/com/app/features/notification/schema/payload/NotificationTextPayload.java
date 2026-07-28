package com.app.features.notification.schema.payload;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationTextPayload {

    @NotBlank
    private String titleKey;

    @NotNull
    private List<Object> titleArguments = List.of();

    @NotBlank
    private String contentKey;

    @NotNull
    private List<Object> contentArguments = List.of();
}
