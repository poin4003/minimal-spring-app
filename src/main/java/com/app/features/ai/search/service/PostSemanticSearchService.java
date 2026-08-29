package com.app.features.ai.search.service;

import com.app.features.ai.search.schema.model.PostSemanticSearchResult;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public interface PostSemanticSearchService {

    PostSemanticSearchResult search(
            @NotBlank @Size(max = 2000) String query);

    PostSemanticSearchResult search(
            @NotBlank @Size(max = 2000) String query,
            @Positive int limit);
}
