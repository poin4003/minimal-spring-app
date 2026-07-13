package com.app.features.ui.web.support;

import java.util.Map;

public record UiFormSubmitResult(
        boolean success,
        Map<String, String> fieldErrors) {

    public static UiFormSubmitResult ok() {
        return new UiFormSubmitResult(true, Map.of());
    }

    public static UiFormSubmitResult fail(Map<String, String> fieldErrors) {
        return new UiFormSubmitResult(false, fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors));
    }
}
