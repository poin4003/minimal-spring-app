package com.app.features.post.videopost.schema.payload;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateVideoPostPayload {

    @NotBlank(message = "{validation.video.title.required}")
    @Size(max = 255, message = "{validation.video.title.tooLong}")
    private String title;

    @Size(max = 10_000, message = "{validation.video.description.tooLong}")
    private String description;

    @NotNull(message = "{validation.video.media.required}")
    private UUID sourceMediaId;
}
