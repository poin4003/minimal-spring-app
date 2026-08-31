package com.app.features.ai.search.schema.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostSearchPayload {

    @NotBlank(message = "{validation.ai.search.query.required}")
    @Size(
            max = 2000,
            message = "{validation.ai.search.query.max}")
    private String query;
}
