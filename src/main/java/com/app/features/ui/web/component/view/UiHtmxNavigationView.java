package com.app.features.ui.web.component.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiHtmxNavigationView {

    private final String target;
    private final String select;

    @Builder.Default
    private final String swap = "outerHTML";

    @Builder.Default
    private final boolean pushUrl = true;

    public static UiHtmxNavigationView forComponent(String componentId) {
        String selector = "#" + componentId;
        return UiHtmxNavigationView.builder()
                .target(selector)
                .select(selector)
                .build();
    }
}
