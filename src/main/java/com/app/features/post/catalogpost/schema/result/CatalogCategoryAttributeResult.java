package com.app.features.post.catalogpost.schema.result;

import java.util.UUID;

import lombok.Data;

@Data
public class CatalogCategoryAttributeResult {

    private UUID id;
    private CatalogAttributeDefinitionResult attribute;
    private boolean required;
    private boolean filterable;
    private boolean multipleValuesAllowed;
    private boolean customOptionAllowed;
    private int sortOrder;
}
