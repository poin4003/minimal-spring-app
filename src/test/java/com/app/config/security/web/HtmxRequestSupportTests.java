package com.app.config.security.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class HtmxRequestSupportTests {

    @Test
    void disablesBothHtmxHistoryStrategies() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        HtmxRequestSupport.disableHistory(response);

        assertThat(response.getHeader("HX-Push-Url")).isEqualTo("false");
        assertThat(response.getHeader("HX-Replace-Url")).isEqualTo("false");
    }
}
