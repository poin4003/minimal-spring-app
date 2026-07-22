package com.app.features.media.schema.payload;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMediaThumbnailPayload {

    @NotNull
    private UUID sourceMediaId;
}
