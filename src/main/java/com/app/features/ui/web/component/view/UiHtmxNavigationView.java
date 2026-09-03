package com.app.features.ui.web.component.view;

import com.app.features.ui.web.enums.UiHtmxHistoryMode;

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
    private final UiHtmxHistoryMode historyMode =
            UiHtmxHistoryMode.REPLACE;

    public Boolean getPushUrl() {
        return switch (historyMode) {
            case PUSH -> Boolean.TRUE;
            case NONE -> Boolean.FALSE;
            case REPLACE -> null;
        };
    }

    public Boolean getReplaceUrl() {
        return switch (historyMode) {
            case REPLACE -> Boolean.TRUE;
            case NONE, PUSH -> Boolean.FALSE;
        };
    }

    public static UiHtmxNavigationView forComponent(String componentId) {
        String selector = "#" + componentId;
        return UiHtmxNavigationView.builder()
                .target(selector)
                .select(selector)
                .build();
    }
}
