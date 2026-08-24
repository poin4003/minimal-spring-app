package com.app.features.ai.exceptions;

public class JlamaRuntimeException extends RuntimeException {

    public JlamaRuntimeException(String message) {
        super(message);
    }

    public JlamaRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
