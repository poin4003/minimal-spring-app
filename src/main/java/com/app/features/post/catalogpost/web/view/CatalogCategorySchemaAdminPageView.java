package com.app.features.post.catalogpost.web.view;

import com.app.features.post.catalogpost.schema.result.CatalogCategoryResult;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.view.UiShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CatalogCategorySchemaAdminPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final UiShellView shell;
    private final String basePath;
    private final CatalogCategoryResult category;
    private final UiPaginationView pagination;
}
