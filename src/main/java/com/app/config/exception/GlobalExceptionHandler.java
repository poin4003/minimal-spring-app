package com.app.config.exception;

import java.nio.file.AccessDeniedException;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.app.core.exception.MyException;
import com.app.core.response.ApiResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MyException.class)
    public ResponseEntity<ApiResult<Void>> handleMyException(MyException ex) {
        log.error("MyException [{}]: {}", ex.getError(), ex.getMessage());

        ApiResult<Void> response = ApiResult.error(ex.getError(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatusCode()).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResult<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Login failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResult.error("INVALID_CREDENTIALS", "Incorrect email or password!"));
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    public ResponseEntity<ApiResult<Void>> handleValidationException(Exception ex) {
        String errorDetails = "";

        if (ex instanceof MethodArgumentNotValidException e) {
            errorDetails = e.getBindingResult().getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        } else if (ex instanceof BindException e) {
            errorDetails = e.getBindingResult().getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        }

        log.warn("Validation Error: {}", errorDetails);
        ApiResult<Void> response = ApiResult.error("INVALID_PARAM", "Invalid data: " + errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
            MissingServletRequestPartException.class
    })
    public ResponseEntity<ApiResult<Void>> handleMissingParams(Exception ex) {
        log.warn("Missing Parameter: {}", ex.getMessage());
        ApiResult<Void> response = ApiResult.error("MISSING_PARAM", "Missing param.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({
            AuthorizationDeniedException.class,
            AccessDeniedException.class
    })
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<Void> handleAccessDeniedException(Exception ex) {
        log.warn("[Security] Access Denied: {}", ex.getMessage());

        return ApiResult.error(
                "PERMISSION_ERROR",
                "You are not authorized to perform this action.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleAllUncaughtException(Exception ex) {
        log.error("Unknown Internal Error: ", ex);
        ApiResult<Void> response = ApiResult.error("INTERNAL_SERVER_ERROR", "Unknown system error.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
