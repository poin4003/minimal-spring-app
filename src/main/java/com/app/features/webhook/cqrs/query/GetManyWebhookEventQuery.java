package com.app.features.webhook.cqrs.query;

import java.util.Objects;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.app.features.webhook.cqrs.result.WebhookEventResult;
import com.app.features.webhook.entity.WebhookEventEntity;
import com.app.features.webhook.filter.WebhookEventCriteria;
import com.app.features.webhook.repository.WebhookEventRepository;
import com.app.features.webhook.repository.spec.WebhookEventSpecification;

import an.awesome.pipelinr.Command;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class GetManyWebhookEventQuery implements Command<Page<WebhookEventResult>>, WebhookEventCriteria {

    private String eventType;

    private String businessReference;

    private Pageable pageable;
}

@Component
@RequiredArgsConstructor
class GetManyWebhookEventHandler implements Command.Handler<GetManyWebhookEventQuery, Page<WebhookEventResult>> {

    private final WebhookEventRepository webhookEventRepo;
    private final ModelMapper mapper;

    @Override
    public Page<WebhookEventResult> handle(GetManyWebhookEventQuery query) {
        Pageable pageable = Objects.requireNonNull(query.getPageable());

        Specification<WebhookEventEntity> spec = WebhookEventSpecification.withFilter(query);

        Page<WebhookEventEntity> entityPage = webhookEventRepo.findAll(spec, pageable);

        return entityPage.map(result -> mapper.map(result, WebhookEventResult.class));
    }
}
