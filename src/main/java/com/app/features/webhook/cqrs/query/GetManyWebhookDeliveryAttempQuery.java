package com.app.features.webhook.cqrs.query;

import java.util.Objects;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.app.features.webhook.cqrs.result.WebhookDeliveryAttemptResult;
import com.app.features.webhook.entity.WebhookDeliveryAttemptEntity;
import com.app.features.webhook.repository.WebhookDeliveryAttemptRepository;

import an.awesome.pipelinr.Command;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class GetManyWebhookDeliveryAttempQuery implements Command<Page<WebhookDeliveryAttemptResult>> {

    private Pageable pageable;
}

@Component
@RequiredArgsConstructor
class GetManyWebhookDeliveryAttempHandler
        implements Command.Handler<GetManyWebhookDeliveryAttempQuery, Page<WebhookDeliveryAttemptResult>> {

    private final WebhookDeliveryAttemptRepository webhookAttempRepo;
    private final ModelMapper mapper;

    @Override
    public Page<WebhookDeliveryAttemptResult> handle(GetManyWebhookDeliveryAttempQuery query) {
        Pageable pageable = Objects.requireNonNull(query.getPageable());

        Page<WebhookDeliveryAttemptEntity> entityPage = webhookAttempRepo.findAll(pageable);

        return entityPage.map(result -> mapper.map(result, WebhookDeliveryAttemptResult.class));
    }
}
