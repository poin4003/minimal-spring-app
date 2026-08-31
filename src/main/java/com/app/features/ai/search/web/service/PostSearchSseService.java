package com.app.features.ai.search.web.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.app.core.enums.AppLanguage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public interface PostSearchSseService {

    SseEmitter stream(
            @NotBlank @Size(max = 2000) String query,
            @NotNull AppLanguage responseLanguage,
            boolean summarize);
}
