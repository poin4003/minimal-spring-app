package com.app.features.webhook.cqrs.query;

import java.util.Objects;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.app.features.webhook.cqrs.result.WebhookSubscriptionResult;
import com.app.features.webhook.entity.WebhookSubscriptionEntity;
import com.app.features.webhook.enums.WebhookSubscriptionStatus;
import com.app.features.webhook.filter.WebhookSubsciptionCriteria;
import com.app.features.webhook.repository.WebhookSubscriptionRepository;
import com.app.features.webhook.repository.spec.WebhookSubscriptionSpecification;

import an.awesome.pipelinr.Command;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class GetManyWebhookSubscriptionQuery
        implements Command<Page<WebhookSubscriptionResult>>, WebhookSubsciptionCriteria {

    private String partnerCode;

    private WebhookSubscriptionStatus status;

    private Pageable pageable;
}

@Component
@RequiredArgsConstructor
class GetManyWebhookSubscriptionHandler
        implements Command.Handler<GetManyWebhookSubscriptionQuery, Page<WebhookSubscriptionResult>> {

    private final WebhookSubscriptionRepository webhookSubRepo;
    private final ModelMapper mapper;

    @Override
    public Page<WebhookSubscriptionResult> handle(GetManyWebhookSubscriptionQuery query) {
        Pageable pageable = Objects.requireNonNull(query.getPageable());

        Specification<WebhookSubscriptionEntity> spec = WebhookSubscriptionSpecification.withFilter(query);

        Page<WebhookSubscriptionEntity> entityPage = webhookSubRepo.findAll(spec, pageable);

        return entityPage.map(result -> mapper.map(result, WebhookSubscriptionResult.class));
    }
}
