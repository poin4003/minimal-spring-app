package com.app.core.i18n;

import java.util.List;

import org.springframework.stereotype.Component;

import com.app.core.exception.FieldErrorDescriptor;
import com.app.core.exception.FieldErrorItem;
import com.app.core.exception.MyException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExceptionMessageResolver {

    private final AppMessageResolver messageResolver;

    public String resolve(MyException exception) {
        return messageResolver.get(
                exception.getMessageKey(),
                exception.getMessageArguments().toArray());
    }

    public String resolve(FieldErrorDescriptor fieldError) {
        return messageResolver.get(
                fieldError.messageKey(),
                fieldError.messageArguments().toArray());
    }

    public List<FieldErrorItem> resolveFieldErrors(MyException exception) {
        return exception.getFieldErrors().stream()
                .map(fieldError -> new FieldErrorItem(
                        fieldError.field(),
                        fieldError.code(),
                        resolve(fieldError),
                        fieldError.rejectedValue()))
                .toList();
    }
}
