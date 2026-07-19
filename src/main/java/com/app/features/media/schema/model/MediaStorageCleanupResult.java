package com.app.features.media.schema.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MediaStorageCleanupResult {

    private final int stagingFilesDeleted;

    private final int processingWorkspacesDeleted;

    private final int orphanDirectoriesDeleted;
}
