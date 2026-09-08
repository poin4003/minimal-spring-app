package com.app.features.post.catalogpost.schema.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCatalogAttributeOptionPayload {

    @NotBlank(message = "{validation.catalogOption.label.required}")
    @Size(max = 150, message = "{validation.catalogOption.label.tooLong}")
    private String label;

    @NotNull(message = "{validation.catalog.sortOrder.required}")
    @PositiveOrZero(message = "{validation.catalog.sortOrder.invalid}")
    private Integer sortOrder;
}
