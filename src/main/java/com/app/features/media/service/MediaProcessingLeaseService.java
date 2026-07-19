package com.app.features.media.service;

import java.util.UUID;

public interface MediaProcessingLeaseService {

    boolean acquire(UUID mediaId, UUID executionId);

    void release(UUID mediaId, UUID executionId);
}
