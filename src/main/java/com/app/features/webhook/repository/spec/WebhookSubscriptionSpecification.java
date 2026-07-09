package com.app.features.webhook.repository.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.app.features.webhook.entity.WebhookSubscriptionEntity;
import com.app.features.webhook.entity.WebhookSubscriptionEntity_;
import com.app.features.webhook.filter.WebhookSubsciptionCriteria;

import jakarta.persistence.criteria.Predicate;

public class WebhookSubscriptionSpecification {

    public static Specification<WebhookSubscriptionEntity> withFilter(WebhookSubsciptionCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getPartnerCode() != null) {
                predicates.add(cb.equal(root.get(WebhookSubscriptionEntity_.partnerCode), criteria.getPartnerCode()));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get(WebhookSubscriptionEntity_.status), criteria.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
