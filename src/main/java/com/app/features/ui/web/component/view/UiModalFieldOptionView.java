package com.app.features.ui.web.component.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class UiModalFieldOptionView {

    private final String value;
    private final String label;
    private final boolean selected;
}
