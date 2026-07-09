package com.app.features.webhook.cqrs.command;

import java.util.Objects;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.exception.ExceptionFactory;
import com.app.core.mapper.PatchMapper;
import com.app.features.webhook.cqrs.result.WebhookSubscriptionResult;
import com.app.features.webhook.entity.WebhookSubscriptionEntity;
import com.app.features.webhook.enums.WebhookSubscriptionStatus;
import com.app.features.webhook.repository.WebhookSubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import an.awesome.pipelinr.Command;
import lombok.RequiredArgsConstructor;

public record UpdateWebhookSubcriptionCmd(
        UUID id,
        String url,
        WebhookSubscriptionStatus status,
        Integer maxRpm,
        Integer baseDelaySeconds,
        Integer maxRetries,
        JsonNode config) implements Command<WebhookSubscriptionResult> {
}

@Component
@RequiredArgsConstructor
class UpdateWebhookSubcriptionHandler
        implements Command.Handler<UpdateWebhookSubcriptionCmd, WebhookSubscriptionResult> {

    private final ModelMapper mapper;
    private final PatchMapper patchMapper;
    private final ObjectMapper objectMapper;
    private final WebhookSubscriptionRepository webhookSubRepo;

    @Override
    @Transactional
    public WebhookSubscriptionResult handle(UpdateWebhookSubcriptionCmd cmd) {

        WebhookSubscriptionEntity entity = webhookSubRepo.findById(Objects.requireNonNull(cmd.id()))
                .orElseThrow(() -> ExceptionFactory.notFound(("Webhook sub: " + cmd.id())));

        if (cmd.config() != null && cmd.config().isObject()) {
            JsonNode currentConfig = entity.getConfig();
            ObjectNode finalConfigData;

            if (currentConfig == null || currentConfig.isNull()) {
                finalConfigData = objectMapper.createObjectNode();
            } else {
                finalConfigData = (ObjectNode) currentConfig.deepCopy();
            }

            ObjectNode updateData = (ObjectNode) cmd.config();

            updateData.fieldNames().forEachRemaining(key -> {
                finalConfigData.set(key, updateData.get(key));
            });

            entity.setConfig(finalConfigData);
        }

        patchMapper.typeMap(UpdateWebhookSubcriptionCmd.class, WebhookSubscriptionEntity.class)
                .addMappings(m -> {
                    m.skip(WebhookSubscriptionEntity::setId);
                    m.skip(WebhookSubscriptionEntity::setConfig);
                });

        patchMapper.map(cmd, entity);
        webhookSubRepo.save(Objects.requireNonNull(entity));

        return mapper.map(entity, WebhookSubscriptionResult.class);
    }
}
