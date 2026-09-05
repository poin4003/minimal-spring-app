package com.app.features.ui.web.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class UiHtmxModelAdviceTests {

    private final UiHtmxModelAdvice advice = new UiHtmxModelAdvice();

    @Test
    void exposesHtmxRequestsToPageTemplates() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("HX-Request", "true");

        assertThat(advice.isHtmxRequest(request, response)).isTrue();
        assertThat(response.getHeaders("Vary"))
                .containsExactly("HX-Request", "HX-History-Restore-Request");
    }

    @Test
    void keepsRegularRequestsOnTheFullPageShell() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(advice.isHtmxRequest(request, response)).isFalse();
    }
}
