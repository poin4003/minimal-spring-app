package com.app.config.security.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class HtmxRequestSupport {

    private static final String REQUEST_HEADER = "HX-Request";
    private static final String REDIRECT_HEADER = "HX-Redirect";

    private HtmxRequestSupport() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isHtmxRequest(HttpServletRequest request) {
        return Boolean.parseBoolean(request.getHeader(REQUEST_HEADER));
    }

    public static void redirect(HttpServletResponse response, String path) {
        response.setHeader(REDIRECT_HEADER, path);
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
