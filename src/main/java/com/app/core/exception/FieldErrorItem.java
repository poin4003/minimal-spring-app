package com.app.core.exception;

public record FieldErrorItem(
        String field,
        String code,
        String message,
        Object rejectedValue) {
}
