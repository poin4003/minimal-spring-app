package com.app.features.post.catalogpost.schema.payload;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReplaceCatalogCategoryAttributesPayload {

    @NotNull(message = "{validation.catalogCategory.attributes.required}")
    private List<@Valid CatalogCategoryAttributePayload> attributes =
            new ArrayList<>();
}
