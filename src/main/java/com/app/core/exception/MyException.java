package com.app.core.exception;

import java.util.List;

import lombok.Getter;

@Getter
public class MyException extends RuntimeException {
    private final String error;
    private final int httpStatusCode;
    private final String messageKey;
    private final List<Object> messageArguments;
    private final Object details;
    private final List<FieldErrorDescriptor> fieldErrors;

    public MyException(
            String error,
            int httpStatusCode,
            String messageKey,
            List<Object> messageArguments,
            Object details,
            List<FieldErrorDescriptor> fieldErrors,
            Throwable cause) {
        super(messageKey, cause);
        this.error = error;
        this.httpStatusCode = httpStatusCode;
        this.messageKey = messageKey;
        this.messageArguments = messageArguments == null
                ? List.of()
                : List.copyOf(messageArguments);
        this.details = details;
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }
}
