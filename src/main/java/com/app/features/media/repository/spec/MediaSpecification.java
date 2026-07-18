package com.app.features.media.repository.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.app.features.media.entity.MediaEntity;
import com.app.features.media.entity.MediaEntity_;
import com.app.features.media.schema.filter.MediaFilterCriteria;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.entity.UserBaseEntity_;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

public class MediaSpecification {

    public static Specification<MediaEntity> withFilter(MediaFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.hasOriginalName()) {
                predicates.add(cb.like(
                        cb.lower(root.get(MediaEntity_.originalName)),
                        contains(criteria.getOriginalName())));
            }

            if (criteria.hasPublicKey()) {
                predicates.add(cb.equal(
                        root.get(MediaEntity_.publicKey),
                        criteria.getPublicKey().trim()));
            }

            if (criteria.hasCreatedByEmail()) {
                Join<MediaEntity, UserBaseEntity> creator = root.join(MediaEntity_.createdBy);

                predicates.add(cb.like(
                        cb.lower(creator.get(UserBaseEntity_.email)),
                        contains(criteria.getCreatedByEmail())));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(
                        root.get(MediaEntity_.status),
                        criteria.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
