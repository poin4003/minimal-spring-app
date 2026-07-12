package com.app.features.ui.web.component.view;

import java.util.List;

import com.app.features.ui.web.enums.UiInputType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiModalFieldView {

    private final String name;
    private final String label;
    private final UiInputType type;
    private final String value;
    private final String placeholder;
    private final String helpText;
    private final boolean required;
    private final boolean readOnly;
    private final boolean checked;
    private final int rows;
    private final List<UiModalFieldOptionView> options;
    private final String errorMessage;
}
