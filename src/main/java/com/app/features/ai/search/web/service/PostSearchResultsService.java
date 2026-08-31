package com.app.features.ai.search.web.service;

import com.app.features.ai.search.web.view.PostSearchResultsView;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public interface PostSearchResultsService {

    PostSearchResultsView search(
            @NotBlank @Size(max = 2000) String query);
}
