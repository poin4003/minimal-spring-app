package com.app.features.post.web.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OwnerPostActionType {
    SUBMIT("submit"),
    ARCHIVE("archive"),
    RESTORE_ARCHIVED("restore-archived"),
    DELETE("delete"),
    RESTORE_DELETED("restore-deleted");

    private final String path;
}
