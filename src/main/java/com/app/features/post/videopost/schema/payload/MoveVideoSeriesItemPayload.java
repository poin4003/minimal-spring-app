package com.app.features.post.videopost.schema.payload;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class MoveVideoSeriesItemPayload {

    @NotNull(message = "{validation.videoSeries.position.required}")
    @PositiveOrZero(message = "{validation.videoSeries.position.invalid}")
    private Integer targetPosition;
}
