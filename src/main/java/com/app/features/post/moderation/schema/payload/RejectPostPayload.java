package com.app.features.post.moderation.schema.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectPostPayload {

    @NotBlank(message = "{validation.post.rejectReason.required}")
    @Size(max = 1_000, message = "{validation.post.rejectReason.tooLong}")
    private String reason;
}
