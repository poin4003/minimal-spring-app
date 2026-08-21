package com.app.features.post.aimoderation.exceptions;

public class PostAiModerationClientException extends RuntimeException {

    public PostAiModerationClientException(String message) {
        super(message);
    }

    public PostAiModerationClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
