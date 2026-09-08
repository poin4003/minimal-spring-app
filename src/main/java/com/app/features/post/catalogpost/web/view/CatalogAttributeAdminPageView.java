package com.app.features.post.catalogpost.web.view;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.app.features.post.catalogpost.enums.CatalogAttributeValueType;
import com.app.features.post.catalogpost.schema.result.CatalogAttributeDefinitionResult;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.view.UiShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CatalogAttributeAdminPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final UiShellView shell;
    private final String basePath;
    private final String categoryPath;
    private final UUID editId;
    private final List<CatalogAttributeDefinitionResult> attributes;
    private final List<CatalogAttributeValueType> valueTypes;
    private final UiPaginationView pagination;

    @Builder.Default
    private final Map<String, String> fieldErrors = Map.of();
}
