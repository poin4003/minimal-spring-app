package com.app.features.webhook.api.v1.controller;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.core.constant.PermissionConstants;
import com.app.core.response.ApiResult;
import com.app.features.webhook.api.v1.dto.query.WebhookEventFilterDto;
import com.app.features.webhook.api.v1.dto.query.WebhookSubscriptionFilterDto;
import com.app.features.webhook.api.v1.dto.request.UpdateWebhookSubscriptionDto;
import com.app.features.webhook.api.v1.dto.response.WebhookDeliveryAttempDto;
import com.app.features.webhook.api.v1.dto.response.WebhookEventDto;
import com.app.features.webhook.api.v1.dto.response.WebhookSubscriptionDto;
import com.app.features.webhook.cqrs.command.UpdateWebhookSubcriptionCmd;
import com.app.features.webhook.cqrs.query.GetManyWebhookDeliveryAttempQuery;
import com.app.features.webhook.cqrs.query.GetManyWebhookEventQuery;
import com.app.features.webhook.cqrs.query.GetManyWebhookSubscriptionQuery;
import com.app.features.webhook.cqrs.result.WebhookDeliveryAttemptResult;
import com.app.features.webhook.cqrs.result.WebhookEventResult;
import com.app.features.webhook.cqrs.result.WebhookSubscriptionResult;

import an.awesome.pipelinr.Pipeline;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhook")
@Tag(name = "WEBHOOK Management V1", description = "Webhook docs")
public class WebhookController {

    private final Pipeline pipeline;
    private final ModelMapper mapper;

    @PatchMapping("/subscription/{id}")
    @Operation(summary = "Update webhook subscription", description = "Update webhook subscription")
    @Secured(PermissionConstants.WEBHOOK_MANAGE)
    public ApiResult<WebhookSubscriptionDto> updateWebhookSubcription(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWebhookSubscriptionDto req) {
        UpdateWebhookSubcriptionCmd cmd = mapper.map(req, UpdateWebhookSubcriptionCmd.class);

        WebhookSubscriptionResult result = pipeline.send(cmd);

        return ApiResult.ok(mapper.map(result, WebhookSubscriptionDto.class), "Update webhook subscription success!");
    }

    @GetMapping("/subscription")
    @Operation(summary = "Get many webhook subscription", description = "Get many webhook subscription")
    @Secured(PermissionConstants.WEBHOOK_MANAGE)
    public ApiResult<Page<WebhookSubscriptionDto>> getManyWebhookSubscription(
            @ParameterObject WebhookSubscriptionFilterDto req,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        GetManyWebhookSubscriptionQuery query = mapper.map(req, GetManyWebhookSubscriptionQuery.class);

        query.setPageable(pageable);

        Page<WebhookSubscriptionResult> results = pipeline.send(query);

        Page<WebhookSubscriptionDto> response = results.map(result -> mapper.map(result, WebhookSubscriptionDto.class));

        return ApiResult.ok(response, "Get many webhook subscription success!");
    }

    @GetMapping("/event")
    @Operation(summary = "Get many webhook event", description = "Get many webhook event")
    @Secured(PermissionConstants.WEBHOOK_MANAGE)
    public ApiResult<Page<WebhookEventDto>> getManyWebhookEvent(
            @ParameterObject WebhookEventFilterDto req,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        GetManyWebhookEventQuery query = mapper.map(req, GetManyWebhookEventQuery.class);

        query.setPageable(pageable);

        Page<WebhookEventResult> results = pipeline.send(query);

        Page<WebhookEventDto> response = results.map(result -> mapper.map(result, WebhookEventDto.class));

        return ApiResult.ok(response, "Get many webhook event success!");
    }

    @GetMapping("/attempt")
    @Operation(summary = "Get many webhook attempt", description = "Get many webhook attempt")
    @Secured(PermissionConstants.WEBHOOK_MANAGE)
    public ApiResult<Page<WebhookDeliveryAttempDto>> getManyWebhookAttempt(
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        GetManyWebhookDeliveryAttempQuery query = new GetManyWebhookDeliveryAttempQuery();
        query.setPageable(pageable);

        Page<WebhookDeliveryAttemptResult> results = pipeline.send(query);

        Page<WebhookDeliveryAttempDto> response = results
                .map(result -> mapper.map(result, WebhookDeliveryAttempDto.class));

        return ApiResult.ok(response, "Get many webhook attempt success!");
    }
}