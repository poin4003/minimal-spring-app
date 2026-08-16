package com.app.config.security.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class HtmxRequestSupport {

    private static final String REQUEST_HEADER = "HX-Request";
    private static final String REDIRECT_HEADER = "HX-Redirect";
    private static final String REPLACE_URL_HEADER = "HX-Replace-Url";
    private static final String TRIGGER_HEADER = "HX-Trigger";
    private static final String EMPTY_RESPONSE_VIEW =
            "fragments/components/htmx-response :: empty";

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

    public static void replaceUrl(HttpServletResponse response, String path) {
        response.setHeader(REPLACE_URL_HEADER, path);
    }

    public static void trigger(
            HttpServletResponse response,
            String eventName) {
        response.setHeader(TRIGGER_HEADER, eventName);
    }

    public static String redirectView(
            HttpServletRequest request,
            HttpServletResponse response,
            String path) {
        if (isHtmxRequest(request)) {
            redirect(response, path);
            return EMPTY_RESPONSE_VIEW;
        }

        return "redirect:" + path;
    }
}
