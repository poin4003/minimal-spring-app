package com.app.features.post.service;

import com.app.features.post.schema.model.PostCleanupResult;

public interface PostMaintenanceService {

    PostCleanupResult cleanupExpiredPosts();
}
