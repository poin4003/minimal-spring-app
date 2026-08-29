package com.app.features.ai.search.service;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public interface PostSearchSyncService {

    void synchronize(@NotNull UUID postId);
}
