package com.app.config.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.app.core.exception.FieldErrorItem;
import com.app.core.exception.MyException;
import com.app.core.response.ApiResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(MyException.class)
    public ResponseEntity<ApiResult<Void>> handleMyException(MyException ex) {
        log.error("MyException [{}]: {}", ex.getError(), ex.getMessage());

        return ResponseEntity.status(ex.getHttpStatusCode())
                .body(ApiResult.error(ex.getError(), ex.getMessage(), ex.getFieldErrors()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResult<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Login failed: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResult.error("INVALID_CREDENTIALS", "Incorrect email or password!"));
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    public ResponseEntity<ApiResult<Void>> handleValidationException(Exception ex) {
        BindingResult bindingResult = ex instanceof MethodArgumentNotValidException methodEx
                ? methodEx.getBindingResult()
                : ((BindException) ex).getBindingResult();

        List<FieldErrorItem> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(err -> new FieldErrorItem(
                        err.getField(),
                        err.getCode(),
                        err.getDefaultMessage(),
                        err.getRejectedValue()))
                .toList();

        String errorDetails = fieldErrors.stream()
                .map(item -> item.field() + ": " + item.message())
                .collect(Collectors.joining(", "));

        log.warn("Validation Error: {}", errorDetails);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(
                        "COMMON_VALIDATION_ERROR",
                        "Invalid params: " + errorDetails,
                        fieldErrors));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
            MissingServletRequestPartException.class
    })
    public ResponseEntity<ApiResult<Void>> handleMissingParams(Exception ex) {
        log.warn("Missing Parameter: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error("MISSING_PARAM", "Missing param."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResult.error("NOT_FOUND", "Resource not found."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleAllUncaughtException(Exception ex) {
        log.error("Unknown Internal Error: ", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error("INTERNAL_SERVER_ERROR", "Unknown system error."));
    }
}
