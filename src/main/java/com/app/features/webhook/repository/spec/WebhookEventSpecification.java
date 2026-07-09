package com.app.features.webhook.repository.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.app.features.webhook.entity.WebhookEventEntity;
import com.app.features.webhook.entity.WebhookEventEntity_;
import com.app.features.webhook.filter.WebhookEventCriteria;

import jakarta.persistence.criteria.Predicate;

public class WebhookEventSpecification {

    public static Specification<WebhookEventEntity> withFilter(WebhookEventCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getBusinessReference() != null) {
                predicates.add(
                        cb.equal(root.get(WebhookEventEntity_.businessReference), criteria.getBusinessReference()));
            }

            if (criteria.getEventType() != null) {
                predicates.add(cb.equal(root.get(WebhookEventEntity_.eventType), criteria.getEventType()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
