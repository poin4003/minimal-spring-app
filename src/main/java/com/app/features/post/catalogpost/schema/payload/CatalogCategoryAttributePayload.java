package com.app.features.post.catalogpost.schema.payload;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CatalogCategoryAttributePayload {

    @NotNull(message = "{validation.catalogCategory.attribute.required}")
    private UUID attributeId;

    private boolean required;
    private boolean filterable;
    private boolean multipleValuesAllowed;
    private boolean customOptionAllowed;

    @PositiveOrZero(message = "{validation.catalog.sortOrder.invalid}")
    private int sortOrder;
}
