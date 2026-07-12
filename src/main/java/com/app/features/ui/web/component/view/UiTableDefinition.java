package com.app.features.ui.web.component.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiTableDefinition {

    @Builder.Default
    private final String emptyMessage = "No records found.";

    private final String title;
    private final String description;
    private final UiPaginationView pagination;
}
