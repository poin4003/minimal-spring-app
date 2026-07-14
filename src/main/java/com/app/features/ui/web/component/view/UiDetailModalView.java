package com.app.features.ui.web.component.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiDetailModalView {

    private final String id;
    private final String title;
    private final String listTitle;
    private final List<UiDetailItemView> items;
    private final String emptyMessage;
}
