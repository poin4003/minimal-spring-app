package com.app.features.ai.vision.exceptions;

public class AiVisionRuntimeException extends RuntimeException {

    public AiVisionRuntimeException(String message) {
        super(message);
    }

    public AiVisionRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
