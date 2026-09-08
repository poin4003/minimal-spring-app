package com.app.features.post.catalogpost.schema.payload;

import com.app.features.post.catalogpost.enums.CatalogAttributeValueType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCatalogAttributePayload {

    @NotBlank(message = "{validation.catalogAttribute.name.required}")
    @Size(max = 150, message = "{validation.catalogAttribute.name.tooLong}")
    private String name;

    private String description;

    @NotNull(message = "{validation.catalogAttribute.valueType.required}")
    private CatalogAttributeValueType valueType;

    @Size(max = 32, message = "{validation.catalogAttribute.unit.tooLong}")
    private String unit;

    private boolean searchable = true;
}
