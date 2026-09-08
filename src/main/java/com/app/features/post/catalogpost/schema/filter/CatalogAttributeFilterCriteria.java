package com.app.features.post.catalogpost.schema.filter;

import com.app.core.enums.RecordStatus;
import com.app.features.post.catalogpost.enums.CatalogAttributeValueType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CatalogAttributeFilterCriteria {

    private String text;
    private RecordStatus status;
    private CatalogAttributeValueType valueType;
    private Boolean searchable;
}
