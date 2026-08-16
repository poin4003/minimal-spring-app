package com.app.features.post.videopost.schema.payload;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateVideoSeriesPostsPayload {

    @NotEmpty(message = "{validation.videoSeries.posts.required}")
    @Size(max = 100, message = "{validation.videoSeries.posts.tooMany}")
    private List<@NotNull(message = "{validation.videoSeries.post.required}") @Valid CreateVideoPostPayload> videoPosts = List.of();
}
