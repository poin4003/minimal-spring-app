package com.app.features.ui.web.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class UiHtmxModelAdviceTests {

    private final UiHtmxModelAdvice advice = new UiHtmxModelAdvice();

    @Test
    void exposesHtmxRequestsToPageTemplates() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("HX-Request", "true");

        assertThat(advice.isHtmxRequest(request)).isTrue();
    }

    @Test
    void keepsRegularRequestsOnTheFullPageShell() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(advice.isHtmxRequest(request)).isFalse();
    }
}
