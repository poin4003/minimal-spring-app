package com.app.features.post.videopost.schema.payload;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddVideoSeriesItemsPayload {

    @NotEmpty(message = "{validation.videoSeries.items.required}")
    @Size(max = 500, message = "{validation.videoSeries.items.tooMany}")
    private List<@NotNull(message = "{validation.videoSeries.item.video.required}") UUID> videoPostIds = List.of();
}
