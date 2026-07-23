package com.app.features.media.exception;

import org.springframework.http.HttpStatus;

import com.app.core.exception.MyException;

public class InvalidMediaContentException extends MyException {

    public InvalidMediaContentException(String message) {
        super("INVALID_PARAM", HttpStatus.BAD_REQUEST.value(), message);
    }
}
