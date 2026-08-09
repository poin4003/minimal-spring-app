package com.app.features.post.standard.repository.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostEntity_;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.standard.entity.StandardPostEntity;
import com.app.features.post.standard.entity.StandardPostEntity_;
import com.app.features.post.standard.schema.filter.OwnerStandardPostFilterCriteria;
import com.app.features.post.standard.schema.filter.PublicStandardPostFilterCriteria;
import com.app.features.user.entity.UserBaseEntity_;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

public class StandardPostSpecification {

    public static Specification<StandardPostEntity> published(
            PublicStandardPostFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<StandardPostEntity, PostEntity> post = root.join(StandardPostEntity_.post);

            predicates.add(cb.equal(
                    post.get(PostEntity_.lifecycleStatus),
                    PostLifecycleStatus.ACTIVE));
            predicates.add(cb.equal(
                    post.get(PostEntity_.moderationStatus),
                    PostModerationStatus.PUBLISHED));

            if (criteria.getAuthorId() != null) {
                predicates.add(cb.equal(
                        post.get(PostEntity_.author).get(UserBaseEntity_.id),
                        criteria.getAuthorId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<StandardPostEntity> ownedBy(
            UUID ownerId,
            OwnerStandardPostFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<StandardPostEntity, PostEntity> post = root.join(StandardPostEntity_.post);

            predicates.add(cb.equal(
                    post.get(PostEntity_.author).get(UserBaseEntity_.id),
                    ownerId));

            if (criteria.getModerationStatus() != null) {
                predicates.add(cb.equal(
                        post.get(PostEntity_.moderationStatus),
                        criteria.getModerationStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
