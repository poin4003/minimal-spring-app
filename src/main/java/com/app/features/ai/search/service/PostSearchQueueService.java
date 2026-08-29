package com.app.features.ai.search.service;

import java.util.UUID;

public interface PostSearchQueueService {

    boolean enqueue(UUID postId);
}
