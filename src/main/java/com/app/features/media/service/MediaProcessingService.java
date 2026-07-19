package com.app.features.media.service;

import java.util.UUID;

public interface MediaProcessingService {

    void process(UUID mediaId, UUID executionId);
}
