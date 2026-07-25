package com.app.features.notification.repository.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.app.features.notification.entity.NotificationEntity;
import com.app.features.notification.entity.NotificationEntity_;
import com.app.features.notification.enums.NotificationReadState;
import com.app.features.notification.schema.filter.NotificationFilterCriteria;
import com.app.features.user.entity.UserBaseEntity_;

import jakarta.persistence.criteria.Predicate;

public class NotificationSpecification {

    public static Specification<NotificationEntity> withFilter(
            NotificationFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(
                    root.get(NotificationEntity_.recipient).get(UserBaseEntity_.id),
                    criteria.getRecipientId()));

            if (criteria.getType() != null) {
                predicates.add(cb.equal(
                        root.get(NotificationEntity_.type),
                        criteria.getType()));
            }

            if (criteria.getReadState() == NotificationReadState.READ) {
                predicates.add(cb.isNotNull(
                        root.get(NotificationEntity_.readAt)));
            } else if (criteria.getReadState() == NotificationReadState.UNREAD) {
                predicates.add(cb.isNull(
                        root.get(NotificationEntity_.readAt)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
