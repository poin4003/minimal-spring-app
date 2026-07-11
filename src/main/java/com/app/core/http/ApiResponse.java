package com.app.core.http;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private Integer statusCode;
    private String rawBody;
    private T data;
    private boolean isNetworkError; 
}
