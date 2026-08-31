package com.app.features.ai.rag.web.controller;

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
import com.app.features.ai.rag.mapper.PostRagConversationRequestMapper;
import com.app.features.ai.rag.schema.payload.PostRagQuestionPayload;
import com.app.features.ai.rag.web.service.PostRagSseService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.ui.ai-chat-path:/ai-chat}")
public class PostRagChatSseController {

    private final PostRagSseService postRagSseSvc;
    private final PostRagConversationRequestMapper
            postRagConversationRequestMapper;

    @RateLimited(RateLimitPolicy.RAG_QUERY)
    @PostMapping(
            path = "/ask/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @Valid @ModelAttribute PostRagQuestionPayload question,
            Locale locale,
            HttpServletResponse response) {
        response.setHeader(
                HttpHeaders.CACHE_CONTROL,
                "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");

        return postRagSseSvc.stream(
                postRagConversationRequestMapper.toModel(
                        question,
                        locale));
    }
}
