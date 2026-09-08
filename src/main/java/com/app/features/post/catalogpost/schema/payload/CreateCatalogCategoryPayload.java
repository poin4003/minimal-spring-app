package com.app.features.post.catalogpost.schema.payload;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCatalogCategoryPayload {

    private UUID parentId;

    @NotBlank(message = "{validation.catalogCategory.name.required}")
    @Size(max = 150, message = "{validation.catalogCategory.name.tooLong}")
    private String name;

    @NotBlank(message = "{validation.catalogCategory.slug.required}")
    @Size(max = 160, message = "{validation.catalogCategory.slug.tooLong}")
    private String slug;

    private String description;

    @NotNull(message = "{validation.catalog.sortOrder.required}")
    @PositiveOrZero(message = "{validation.catalog.sortOrder.invalid}")
    private Integer sortOrder;
}
