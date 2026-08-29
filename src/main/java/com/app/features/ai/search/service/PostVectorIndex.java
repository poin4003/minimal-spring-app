package com.app.features.ai.search.service;

import java.util.List;
import java.util.UUID;

import com.app.features.ai.search.schema.model.PostVectorDocument;
import com.app.features.ai.search.schema.model.PostVectorSearchHit;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public interface PostVectorIndex {

    void upsert(@NotNull PostVectorDocument document);

    void delete(@NotNull UUID postId);

    String getModelVersion();

    UUID getIndexGeneration();

    List<PostVectorSearchHit> search(
            @NotNull float[] queryVector,
            @Positive int limit);
}
