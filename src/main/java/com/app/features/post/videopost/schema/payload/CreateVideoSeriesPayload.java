package com.app.features.post.videopost.schema.payload;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateVideoSeriesPayload {

    @NotBlank(message = "{validation.videoSeries.title.required}")
    @Size(max = 255, message = "{validation.videoSeries.title.tooLong}")
    private String title;

    @Size(max = 10_000, message = "{validation.videoSeries.description.tooLong}")
    private String description;

    private UUID coverMediaId;
}
