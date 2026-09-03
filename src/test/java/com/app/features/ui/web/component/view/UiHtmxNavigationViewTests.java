package com.app.features.ui.web.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.app.features.ui.web.enums.UiHtmxHistoryMode;

class UiHtmxNavigationViewTests {

    @Test
    void disablesBothHistoryStrategiesForComponentRefreshes() {
        UiHtmxNavigationView navigation = navigation(
                UiHtmxHistoryMode.NONE);

        assertThat(navigation.getPushUrl()).isFalse();
        assertThat(navigation.getReplaceUrl()).isFalse();
    }

    @Test
    void replacesHistoryForPaginationAndFilters() {
        UiHtmxNavigationView navigation = navigation(
                UiHtmxHistoryMode.REPLACE);

        assertThat(navigation.getPushUrl()).isNull();
        assertThat(navigation.getReplaceUrl()).isTrue();
    }

    @Test
    void pushesHistoryOnlyForMeaningfulNavigation() {
        UiHtmxNavigationView navigation = navigation(
                UiHtmxHistoryMode.PUSH);

        assertThat(navigation.getPushUrl()).isTrue();
        assertThat(navigation.getReplaceUrl()).isFalse();
    }

    private UiHtmxNavigationView navigation(
            UiHtmxHistoryMode historyMode) {
        return UiHtmxNavigationView.builder()
                .historyMode(historyMode)
                .build();
    }
}
