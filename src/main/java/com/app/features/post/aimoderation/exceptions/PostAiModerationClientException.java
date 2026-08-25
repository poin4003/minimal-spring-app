package com.app.features.post.aimoderation.exceptions;

public class PostAiModerationClientException extends RuntimeException {

    private final String rawResponse;

    public PostAiModerationClientException(String message) {
        this(message, null, null);
    }

    public PostAiModerationClientException(String message, Throwable cause) {
        this(message, null, cause);
    }

    public PostAiModerationClientException(
            String message,
            String rawResponse) {
        this(message, rawResponse, null);
    }

    public PostAiModerationClientException(
            String message,
            String rawResponse,
            Throwable cause) {
        super(message, cause);
        this.rawResponse = rawResponse;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
