package com.app.features.post.web.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

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

    private static final Map<String, OwnerPostActionType> BY_PATH =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(
                            action -> action.getPath(),
                            action -> action));

    private final String path;

    public static OwnerPostActionType fromPath(String path) {
        return BY_PATH.get(path);
    }
}
