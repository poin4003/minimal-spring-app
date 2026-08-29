package com.app.features.ai.search.exceptions;

public class AiSearchRuntimeException extends RuntimeException {

    public AiSearchRuntimeException(String message) {
        super(message);
    }

    public AiSearchRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
