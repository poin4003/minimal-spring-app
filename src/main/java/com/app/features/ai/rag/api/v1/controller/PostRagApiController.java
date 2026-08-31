package com.app.features.ai.rag.api.v1.controller;

import java.util.Locale;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.config.ratelimit.RateLimitPolicy;
import com.app.config.ratelimit.RateLimited;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.response.ApiResult;
import com.app.features.ai.rag.mapper.PostRagResultMapper;
import com.app.features.ai.rag.schema.model.PostRagResult;
import com.app.features.ai.rag.schema.payload.PostRagQuestionPayload;
import com.app.features.ai.rag.schema.result.PostRagAnswerResult;
import com.app.features.ai.rag.service.PostRagService;
import com.app.features.ai.rag.support.PostRagLanguageResolver;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/rag")
public class PostRagApiController {

    private final PostRagService postRagSvc;
    private final PostRagResultMapper postRagResultMapper;
    private final PostRagLanguageResolver postRagLanguageResolver;
    private final AppMessageResolver messageResolver;

    @RateLimited(RateLimitPolicy.RAG_QUERY)
    @PostMapping("/ask")
    public ApiResult<PostRagAnswerResult> answer(
            @Valid @RequestBody PostRagQuestionPayload payload,
            Locale locale) {
        PostRagResult result = postRagSvc.answer(
                payload.getQuestion(),
                postRagLanguageResolver.resolve(locale));
        return ApiResult.ok(
                postRagResultMapper.toResult(result),
                messageResolver.get("api.ai.rag.answer.success"));
    }
}
