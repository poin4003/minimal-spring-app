package com.app.features.media.service;

import com.app.features.media.schema.model.KnownMediaCleanupResult;

public interface MediaMaintenanceService {

    KnownMediaCleanupResult cleanupKnownMedia();
}
