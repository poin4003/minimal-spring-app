package com.app.features.webhook.http;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WebhookHttpResponse<T> {
    private Integer statusCode;
    private String rawBody;
    private T data;
    private boolean isNetworkError; 
}
