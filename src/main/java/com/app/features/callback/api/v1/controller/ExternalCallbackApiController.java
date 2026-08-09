package com.app.features.callback.api.v1.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.core.i18n.AppMessageResolver;
import com.app.core.response.ApiResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/callback")
public class ExternalCallbackApiController {

    private final AppMessageResolver messageResolver;

    @PostMapping
    public ApiResult<Void> receive(
            @RequestHeader(
                    value = HttpHeaders.CONTENT_TYPE,
                    required = false)
            String contentType,
            @RequestBody(required = false)
            String payload) {
        log.info(
                "External callback received [contentType={}, payload={}]",
                contentType,
                payload);

        return ApiResult.ok(
                null,
                messageResolver.get("api.callback.receive.success"));
    }
}
