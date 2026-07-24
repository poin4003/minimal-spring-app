package com.app.features.ui.web.component.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiBreadcrumbView {

    private final List<UiBreadcrumbItemView> items;
}
