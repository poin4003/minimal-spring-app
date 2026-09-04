package com.app.features.ui.web.support;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.app.config.security.web.HtmxRequestSupport;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class UiHtmxModelAdvice {

    public static final String HTMX_REQUEST_ATTRIBUTE = "htmxRequest";

    @ModelAttribute(HTMX_REQUEST_ATTRIBUTE)
    boolean isHtmxRequest(HttpServletRequest request) {
        return HtmxRequestSupport.isHtmxRequest(request);
    }
}
