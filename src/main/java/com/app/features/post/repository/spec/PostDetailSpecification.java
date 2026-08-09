package com.app.features.post.repository.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostEntity_;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.user.entity.UserBaseEntity_;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SingularAttribute;

public final class PostDetailSpecification {

    private PostDetailSpecification() {
    }

    public static <D> Specification<D> published(
            SingularAttribute<D, PostEntity> postAttribute,
            UUID authorId) {
        return (root, query, cb) -> {
            Join<D, PostEntity> post = root.join(postAttribute);
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(
                    post.get(PostEntity_.lifecycleStatus),
                    PostLifecycleStatus.ACTIVE));
            predicates.add(cb.equal(
                    post.get(PostEntity_.moderationStatus),
                    PostModerationStatus.PUBLISHED));

            if (authorId != null) {
                predicates.add(cb.equal(
                        post.get(PostEntity_.author)
                                .get(UserBaseEntity_.id),
                        authorId));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static <D> Specification<D> ownedBy(
            SingularAttribute<D, PostEntity> postAttribute,
            UUID ownerId,
            PostLifecycleStatus lifecycleStatus,
            PostModerationStatus moderationStatus) {
        return (root, query, cb) -> {
            Join<D, PostEntity> post = root.join(postAttribute);
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(
                    post.get(PostEntity_.author)
                            .get(UserBaseEntity_.id),
                    ownerId));

            if (lifecycleStatus != null) {
                predicates.add(cb.equal(
                        post.get(PostEntity_.lifecycleStatus),
                        lifecycleStatus));
            } else if (moderationStatus != null) {
                predicates.add(cb.equal(
                        post.get(PostEntity_.lifecycleStatus),
                        PostLifecycleStatus.ACTIVE));
            } else {
                predicates.add(cb.notEqual(
                        post.get(PostEntity_.lifecycleStatus),
                        PostLifecycleStatus.DELETED));
            }

            if (moderationStatus != null) {
                predicates.add(cb.equal(
                        post.get(PostEntity_.moderationStatus),
                        moderationStatus));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
