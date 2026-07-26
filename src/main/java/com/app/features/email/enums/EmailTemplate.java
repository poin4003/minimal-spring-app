package com.app.features.email.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailTemplate {
    SAMPLE("email/sample"),
    MEDIA_NOTIFICATION("email/media-notification");

    private final String path;
}
