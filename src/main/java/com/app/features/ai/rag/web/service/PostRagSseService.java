package com.app.features.ai.rag.web.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.app.features.ai.rag.schema.model.PostRagConversationRequest;

import jakarta.validation.constraints.NotNull;

public interface PostRagSseService {

    SseEmitter stream(
            @NotNull PostRagConversationRequest request);
}
