package com.app.features.ai.search.web.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.config.ratelimit.RateLimitPolicy;
import com.app.config.ratelimit.RateLimited;
import com.app.features.ai.search.schema.payload.PostSearchPayload;
import com.app.features.ai.search.web.service.PostSearchResultsService;
import com.app.features.ai.search.web.view.PostSearchResultsView;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.ui.search-path:/search}")
public class PostSearchResultsController {

    private final PostSearchResultsService postSearchResultsSvc;

    @RateLimited(RateLimitPolicy.SEARCH_QUERY)
    @PostMapping(
            path = "/results",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public PostSearchResultsView search(
            @Valid @ModelAttribute PostSearchPayload payload) {
        return postSearchResultsSvc.search(payload.getQuery());
    }
}
