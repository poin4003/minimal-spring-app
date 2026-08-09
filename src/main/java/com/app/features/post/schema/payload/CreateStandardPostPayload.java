package com.app.features.post.schema.payload;

import java.util.List;
import java.util.UUID;

import org.springframework.util.StringUtils;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateStandardPostPayload {

    @Size(max = 10_000, message = "{validation.post.content.tooLong}")
    private String content;

    @NotNull
    @Size(max = 20, message = "{validation.post.media.tooMany}")
    private List<@NotNull UUID> mediaIds = List.of();

    @AssertTrue(message = "{validation.post.content.required}")
    public boolean isContentAvailable() {
        return StringUtils.hasText(content) || (mediaIds != null && !mediaIds.isEmpty());
    }
}
