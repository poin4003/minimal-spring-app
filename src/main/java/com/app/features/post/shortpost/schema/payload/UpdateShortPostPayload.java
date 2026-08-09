package com.app.features.post.shortpost.schema.payload;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateShortPostPayload {

    @Size(max = 1000, message = "{validation.short.caption.tooLong}")
    private String caption;

    @NotNull(message = "{validation.short.media.required}")
    private UUID mediaId;
}
