package com.app.features.post.catalogpost.web.view;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.app.features.post.catalogpost.schema.result.CatalogCategoryResult;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.view.UiShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CatalogCategoryAdminPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final UiShellView shell;
    private final String basePath;
    private final String attributePath;
    private final UUID editId;
    private final List<CatalogCategoryResult> categories;
    private final List<CatalogCategoryResult> parentOptions;
    private final String parentText;
    private final UiPaginationView pagination;

    @Builder.Default
    private final Map<String, String> fieldErrors = Map.of();
}
