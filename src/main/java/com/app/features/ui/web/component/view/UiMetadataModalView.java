package com.app.features.ui.web.component.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiMetadataModalView {

    public static final String ATTRIBUTE = "modal";

    private final String id;
    private final String title;
    private final List<UiMetadataItemView> items;
}
