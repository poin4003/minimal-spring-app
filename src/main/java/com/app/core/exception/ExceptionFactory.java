package com.app.core.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

public final class ExceptionFactory {

    private ExceptionFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // --- Auth Errors (401) ---
    public static MyException invalidToken(String messageKey, Object... messageArguments) {
        return new MyException(
                "INVALID_TOKEN",
                HttpStatus.UNAUTHORIZED.value(),
                messageKey,
                List.of(messageArguments),
                null,
                List.of(),
                null);
    }

    public static MyException accessTokenExpired(String messageKey, Object... messageArguments) {
        return new MyException(
                "ACCESS_TOKEN_EXPIRED",
                HttpStatus.UNAUTHORIZED.value(),
                messageKey,
                List.of(messageArguments),
                null,
                List.of(),
                null);
    }

    // --- Business Errors (400, 404) ---
    public static MyException notFound(String messageKey, Object... messageArguments) {
        return new MyException(
                "RESOURCE_NOT_FOUND",
                HttpStatus.NOT_FOUND.value(),
                messageKey,
                List.of(messageArguments),
                null,
                List.of(),
                null);
    }

    public static MyException alreadyExists(String messageKey, Object... messageArguments) {
        return new MyException(
                "RESOURCE_ALREADY_EXISTS",
                HttpStatus.BAD_REQUEST.value(),
                messageKey,
                List.of(messageArguments),
                null,
                List.of(),
                null);
    }

    public static MyException alreadyExists(
            String field,
            Object rejectedValue,
            String messageKey,
            Object... messageArguments) {
        FieldErrorDescriptor fieldError = new FieldErrorDescriptor(
                field,
                "ALREADY_EXISTS",
                messageKey,
                List.of(messageArguments),
                rejectedValue);

        return new MyException(
                "RESOURCE_ALREADY_EXISTS",
                HttpStatus.BAD_REQUEST.value(),
                messageKey,
                List.of(messageArguments),
                null,
                List.of(fieldError),
                null);
    }

    public static MyException invalidParam(String messageKey, Object... messageArguments) {
        return new MyException(
                "INVALID_PARAM",
                HttpStatus.BAD_REQUEST.value(),
                messageKey,
                List.of(messageArguments),
                null,
                List.of(),
                null);
    }

    public static MyException validationError(
            String messageKey,
            List<FieldErrorDescriptor> fieldErrors,
            Object... messageArguments) {
        return new MyException(
                "COMMON_VALIDATION_ERROR",
                HttpStatus.BAD_REQUEST.value(),
                messageKey,
                List.of(messageArguments),
                null,
                fieldErrors,
                null);
    }

    // --- Security Error (401, 403)
    public static MyException tokenInvalid(String messageKey, Object... messageArguments) {
        return new MyException(
                "TOKEN_INVALID",
                HttpStatus.UNAUTHORIZED.value(),
                messageKey,
                List.of(messageArguments),
                null,
                List.of(),
                null);
    }

    public static MyException invalidCredentials() {
        return new MyException(
                "INVALID_CREDENTIALS",
                HttpStatus.UNAUTHORIZED.value(),
                "error.auth.invalidCredentials",
                List.of(),
                null,
                List.of(),
                null);
    }

    public static MyException permissionError(String messageKey, Object... messageArguments) {
        return new MyException(
                "PERMISSION_ERROR",
                HttpStatus.FORBIDDEN.value(),
                messageKey,
                List.of(messageArguments),
                null,
                List.of(),
                null);
    }

    public static MyException rateLimitExceeded(String messageKey, Object... messageArguments) {
        return new MyException(
                "RATE_LIMIT_EXCEEDED",
                HttpStatus.TOO_MANY_REQUESTS.value(),
                messageKey,
                List.of(messageArguments),
                null,
                List.of(),
                null);
    }

    // --- Infrastructure/System Errors (500) ---
    public static MyException serverError(String messageKey, Object... messageArguments) {
        return new MyException(
                "INTERNAL_SERVER_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                messageKey,
                List.of(messageArguments),
                null,
                List.of(),
                null);
    }

    public static MyException serverError(
            String messageKey,
            Throwable cause,
            Object... messageArguments) {
        return new MyException(
                "INTERNAL_SERVER_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                messageKey,
                List.of(messageArguments),
                null,
                List.of(),
                cause);
    }

    public static MyException importSimError(String messageKey, Object... messageArguments) {
        return new MyException(
                "IMPORT_SIM_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                messageKey,
                List.of(messageArguments),
                null,
                List.of(),
                null);
    }
}
