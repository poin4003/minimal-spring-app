package com.app.core.exception;

import lombok.Getter;

@Getter
public class MyException extends RuntimeException {
    private final String error;
    private final int httpStatusCode;
    private final Object details;

    public MyException(String error, int httpStatusCode, String message) {
        this(error, httpStatusCode, message, null);
    }

    public MyException(String error, int httpStatusCode, String message, Object details) {
        super(message);
        this.error = error;
        this.httpStatusCode = httpStatusCode;
        this.details = details;
    }
}
