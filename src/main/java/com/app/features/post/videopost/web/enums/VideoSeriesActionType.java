package com.app.features.post.videopost.web.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VideoSeriesActionType {
    ARCHIVE("archive"),
    RESTORE_ARCHIVED("restore-archived"),
    DELETE("delete"),
    RESTORE_DELETED("restore-deleted");

    private final String path;
}
