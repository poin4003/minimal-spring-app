package com.app.features.media.validation.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidatedMediaFile {

    private final String contentType;

    private final Integer originalWidth;

    private final Integer originalHeight;

    private final Long durationMillis;
}
