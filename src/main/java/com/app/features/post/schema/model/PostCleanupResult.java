package com.app.features.post.schema.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PostCleanupResult {

    private final int deletedCount;

    private final int rejectedCount;
}
