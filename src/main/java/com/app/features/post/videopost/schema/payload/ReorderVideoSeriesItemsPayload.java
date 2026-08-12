package com.app.features.post.videopost.schema.payload;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReorderVideoSeriesItemsPayload {

    @NotNull(message = "{validation.videoSeries.items.required}")
    @Size(max = 500, message = "{validation.videoSeries.items.tooMany}")
    private List<@NotNull(message = "{validation.videoSeries.item.required}") UUID> seriesItemIds = List.of();
}
