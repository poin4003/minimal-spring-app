package com.app.core.exception;

import java.util.List;

public record FieldErrorDescriptor(
        String field,
        String code,
        String messageKey,
        List<Object> messageArguments,
        Object rejectedValue) {

    public FieldErrorDescriptor {
        messageArguments = messageArguments == null
                ? List.of()
                : List.copyOf(messageArguments);
    }
}
