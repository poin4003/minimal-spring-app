package com.app.features.post.catalogpost.web.view;

import java.util.UUID;

import com.app.core.enums.RecordStatus;
import com.app.features.post.catalogpost.enums.CatalogAttributeValueType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CatalogCategoryAttributeAssignmentForm {

    @NotNull
    private UUID attributeId;

    private String key;
    private String name;
    private CatalogAttributeValueType valueType;
    private RecordStatus status;
    private boolean selected;
    private boolean required;
    private boolean filterable;
    private boolean multipleValuesAllowed;
    private boolean customOptionAllowed;

    @PositiveOrZero(message = "{validation.catalog.sortOrder.invalid}")
    private int sortOrder;
}
