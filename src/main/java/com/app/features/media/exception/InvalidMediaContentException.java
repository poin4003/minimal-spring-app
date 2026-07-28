package com.app.features.media.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

import com.app.core.exception.MyException;

public class InvalidMediaContentException extends MyException {

    public InvalidMediaContentException(
            String messageKey,
            Object... messageArguments) {
        super(
                "INVALID_PARAM",
                HttpStatus.BAD_REQUEST.value(),
                messageKey,
                List.of(messageArguments),
                null,
                List.of(),
                null);
    }
}
