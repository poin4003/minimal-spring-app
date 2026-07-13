package com.app.core.response;

import java.util.List;

import com.app.core.exception.FieldErrorItem;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> {
    private boolean success;
    private String error;
    private String message;
    private T result;
    private List<FieldErrorItem> fieldErrors;
    private long timestamp = System.currentTimeMillis();

    public static <T> ApiResult<T> ok(T result, String message) {
        ApiResult<T> res = new ApiResult<>();
        res.setSuccess(true);
        res.setResult(result);
        res.setMessage(message);
        return res;
    }

    public static <T> ApiResult<Void> error(String error, String message) {
        return error(error, message, null);
    }

    public static ApiResult<Void> error(String error, String message, List<FieldErrorItem> fieldErrors) {
        ApiResult<Void> res = new ApiResult<>();
        res.setSuccess(false);
        res.setError(error);
        res.setMessage(message);
        res.setFieldErrors(fieldErrors == null || fieldErrors.isEmpty() ? null : List.copyOf(fieldErrors));
        return res;
    }
}
