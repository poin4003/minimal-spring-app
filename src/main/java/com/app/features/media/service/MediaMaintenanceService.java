package com.app.features.media.service;

import com.app.features.media.schema.model.KnownMediaCleanupResult;
import com.app.features.media.schema.model.MediaStorageCleanupResult;

public interface MediaMaintenanceService {

    KnownMediaCleanupResult cleanupKnownMedia();

    MediaStorageCleanupResult cleanupStorage();
}
