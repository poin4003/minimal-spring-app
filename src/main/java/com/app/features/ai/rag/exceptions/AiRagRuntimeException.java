package com.app.features.ai.rag.exceptions;

public class AiRagRuntimeException extends RuntimeException {

    public AiRagRuntimeException(String message) {
        super(message);
    }

    public AiRagRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
