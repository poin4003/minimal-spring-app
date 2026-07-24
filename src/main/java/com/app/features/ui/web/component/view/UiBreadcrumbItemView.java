package com.app.features.ui.web.component.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiBreadcrumbItemView {

    private final String label;
    private final String path;
    private final boolean active;
}
