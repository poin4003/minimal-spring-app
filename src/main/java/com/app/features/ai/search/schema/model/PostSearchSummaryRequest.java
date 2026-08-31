package com.app.features.ai.search.schema.model;

import com.app.core.enums.AppLanguage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PostSearchSummaryRequest(
        @NotNull @Valid PostSearchResult searchResult,
        @NotNull AppLanguage responseLanguage) {
}
