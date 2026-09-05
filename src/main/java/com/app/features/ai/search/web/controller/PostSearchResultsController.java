package com.app.features.ai.search.web.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.ratelimit.RateLimitPolicy;
import com.app.config.ratelimit.RateLimited;
import com.app.config.security.web.HtmxRequestSupport;
import com.app.config.settings.AppProperties;
import com.app.features.ai.search.schema.payload.PostSearchPayload;
import com.app.features.ai.search.web.service.PostSearchResultsService;
import com.app.features.ai.search.web.view.PostSearchResultsView;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.search-path:/search}")
public class PostSearchResultsController {

    private final PostSearchResultsService postSearchResultsSvc;
    private final AppProperties appProperties;

    @RateLimited(RateLimitPolicy.SEARCH_QUERY)
    @PostMapping(
            path = "/results",
            produces = MediaType.TEXT_HTML_VALUE)
    public String search(
            @Valid @ModelAttribute PostSearchPayload payload,
            HttpServletResponse response,
            Model model) {
        PostSearchResultsView results = postSearchResultsSvc.search(
                payload.getQuery());
        model.addAttribute("results", results);
        HtmxRequestSupport.replaceUrl(
                response,
                UriComponentsBuilder
                        .fromPath(appProperties.getUi().getSearchPath())
                        .queryParam("q", payload.getQuery())
                        .build()
                        .encode()
                        .toUriString());
        return "ai/search/fragments/results :: results";
    }
}
