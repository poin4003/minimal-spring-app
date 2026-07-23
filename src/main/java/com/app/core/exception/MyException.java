package com.app.core.exception;

import java.util.List;

import lombok.Getter;

@Getter
public class MyException extends RuntimeException {
    private final String error;
    private final int httpStatusCode;
    private final Object details;
    private final List<FieldErrorItem> fieldErrors;

    public MyException(String error, int httpStatusCode, String message) {
        this(error, httpStatusCode, message, null, List.of(), null);
    }

    public MyException(String error, int httpStatusCode, String message, Object details) {
        this(error, httpStatusCode, message, details, List.of(), null);
    }

    public MyException(
            String error,
            int httpStatusCode,
            String message,
            Object details,
            List<FieldErrorItem> fieldErrors) {
        this(error, httpStatusCode, message, details, fieldErrors, null);
    }

    public MyException(
            String error,
            int httpStatusCode,
            String message,
            Object details,
            List<FieldErrorItem> fieldErrors,
            Throwable cause) {
        super(message, cause);
        this.error = error;
        this.httpStatusCode = httpStatusCode;
        this.details = details;
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }
}
