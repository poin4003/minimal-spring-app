package com.app.features.post.catalogpost.schema.result;

import java.util.List;

import lombok.Data;

@Data
public class CatalogCategoryFormSchemaResult {

    private CatalogCategoryResult category;

    private List<CatalogCategoryAttributeResult> attributes = List.of();
}
