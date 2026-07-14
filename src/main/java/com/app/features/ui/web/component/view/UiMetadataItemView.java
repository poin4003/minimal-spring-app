package com.app.features.ui.web.component.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiMetadataItemView {

    private final String label;
    private final String value;
    private final boolean monospace;
}
