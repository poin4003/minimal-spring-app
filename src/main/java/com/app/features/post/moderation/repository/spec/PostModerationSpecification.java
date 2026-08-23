package com.app.features.post.moderation.repository.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.app.core.db.BaseAuditEntity_;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostEntity_;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.ModerationPostStatusFilter;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.moderation.schema.filter.ModerationPostFilterCriteria;
import com.app.features.user.entity.UserBaseEntity_;

import jakarta.persistence.criteria.Predicate;

public class PostModerationSpecification {

    public static Specification<PostEntity> filter(
            ModerationPostFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(
                    root.get(PostEntity_.lifecycleStatus),
                    PostLifecycleStatus.ACTIVE));

            if (criteria.getModerationStatus()
                    != ModerationPostStatusFilter.ALL) {
                predicates.add(cb.equal(
                        root.get(PostEntity_.moderationStatus),
                        PostModerationStatus.valueOf(
                                criteria.getModerationStatus().name())));
            }

            if (criteria.getType() != null) {
                predicates.add(cb.equal(
                        root.get(PostEntity_.type),
                        criteria.getType()));
            }

            if (criteria.getAuthorId() != null) {
                predicates.add(cb.equal(
                        root.get(PostEntity_.author).get(UserBaseEntity_.id),
                        criteria.getAuthorId()));
            }

            if (criteria.getCreatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get(BaseAuditEntity_.createdAt),
                        criteria.getCreatedFrom()));
            }

            if (criteria.getCreatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get(BaseAuditEntity_.createdAt),
                        criteria.getCreatedTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
