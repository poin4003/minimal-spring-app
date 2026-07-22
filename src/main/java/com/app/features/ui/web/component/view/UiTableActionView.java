package com.app.features.ui.web.component.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiTableActionView {

    private final String label;
    private final String path;
    private final String partialPath;
    private final String buttonClass;
    private final boolean disabled;

    public boolean loadsModal() {
        return partialPath != null;
    }
}
