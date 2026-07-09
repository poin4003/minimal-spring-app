package com.app.core.exception;

import org.springframework.http.HttpStatus;

public final class ExceptionFactory {

    private ExceptionFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // --- Auth Errors (401) ---
    public static MyException invalidToken(String message) {
        return new MyException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED.value(), message);
    }

    public static MyException accessTokenExpired(String message) {
        return new MyException("ACCESS_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED.value(), message);
    }

    // --- Business Errors (400, 404) ---
    public static MyException notFound(String message) {
        return new MyException("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND.value(), message);
    }

    public static MyException alreadyExists(String message) {
        return new MyException("RESOURCE_ALREADY_EXISTS", HttpStatus.BAD_REQUEST.value(), message);
    }

    public static MyException invalidParam(String message) {
        return new MyException("INVALID_PARAM", HttpStatus.BAD_REQUEST.value(), message);
    }

    // --- Security Error (401, 403)
    public static MyException tokenInvalid(String message) {
        return new MyException("TOKEN_INVALID", HttpStatus.UNAUTHORIZED.value(), message);
    }

    public static MyException permissionError(String message) {
        return new MyException("PERMISSION_ERROR", HttpStatus.FORBIDDEN.value(), message);
    }

    // --- Infrastructure/System Errors (500) ---
    public static MyException serverError(String message) {
        return new MyException("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    public static MyException importSimError(String message) {
        return new MyException("IMPORT_SIM_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }
}
