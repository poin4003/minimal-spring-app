package com.app.features.media.schema.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class KnownMediaCleanupResult {

    private final int failedHlsCleaned;

    private final int missingOriginalsDetected;
}
