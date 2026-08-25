package com.app.features.ai.embedding.exceptions;

public class AiEmbeddingRuntimeException extends RuntimeException {

    public AiEmbeddingRuntimeException(String message) {
        super(message);
    }

    public AiEmbeddingRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
