package com.app.features.media.schema.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StartMediaUploadPayload {

    @NotBlank
    @Size(max = 255)
    private String originalName;

    @Positive
    private long fileSize;
}
