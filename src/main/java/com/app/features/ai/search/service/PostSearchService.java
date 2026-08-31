package com.app.features.ai.search.service;

import com.app.features.ai.search.schema.model.PostSearchRequest;
import com.app.features.ai.search.schema.model.PostSearchResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface PostSearchService {

    PostSearchResult search(
            @NotNull @Valid PostSearchRequest request);
}
