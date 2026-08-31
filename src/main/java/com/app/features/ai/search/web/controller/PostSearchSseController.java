package com.app.features.ai.search.web.controller;

import java.util.Locale;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.app.config.ratelimit.RateLimitPolicy;
import com.app.config.ratelimit.RateLimited;
import com.app.features.ai.search.schema.payload.PostSearchPayload;
import com.app.features.ai.search.web.service.PostSearchSseService;
import com.app.features.ai.support.AiResponseLanguageResolver;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.ui.search-path:/search}")
public class PostSearchSseController {

    private final PostSearchSseService postSearchSseSvc;
    private final AiResponseLanguageResolver aiResponseLanguageResolver;

    @RateLimited(RateLimitPolicy.RAG_QUERY)
    @PostMapping(
            path = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @Valid @ModelAttribute PostSearchPayload payload,
            Locale locale,
            HttpServletResponse response) {
        response.setHeader(
                HttpHeaders.CACHE_CONTROL,
                "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");

        return postSearchSseSvc.stream(
                payload.getQuery(),
                aiResponseLanguageResolver.resolve(locale),
                payload.isSummarize());
    }
}
