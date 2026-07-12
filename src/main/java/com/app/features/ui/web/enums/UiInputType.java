package com.app.features.ui.web.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UiInputType {
    TEXT("text"),
    EMAIL("email"),
    PASSWORD("password"),
    NUMBER("number"),
    TEXTAREA("textarea"),
    SELECT("select"),
    CHECKBOX("checkbox"),
    HIDDEN("hidden");

    private final String htmlInputType;
}
