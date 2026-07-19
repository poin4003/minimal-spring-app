package com.app.features.media.repository.spec;

import java.util.ArrayList;
import java.util.Collection;
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

            if (criteria.getCreatedById() != null) {
                predicates.add(cb.equal(
                        root.get(MediaEntity_.createdBy).get(UserBaseEntity_.id),
                        criteria.getCreatedById()));
            }

            if (criteria.hasCreatedByEmail()) {
                Join<MediaEntity, UserBaseEntity> creator = root.join(MediaEntity_.createdBy);

                predicates.add(cb.like(
                        cb.lower(creator.get(UserBaseEntity_.email)),
                        contains(criteria.getCreatedByEmail())));
            }

            if (criteria.getKind() != null) {
                predicates.add(cb.equal(
                        root.get(MediaEntity_.kind),
                        criteria.getKind()));
            }

            if (criteria.getProcessingStatus() != null) {
                predicates.add(cb.equal(
                        root.get(MediaEntity_.processingStatus),
                        criteria.getProcessingStatus()));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(
                        root.get(MediaEntity_.status),
                        criteria.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<MediaEntity> storageDirectoryIn(
            Collection<String> storageDirectoryKeys) {
        return (root, query, cb) -> {
            if (storageDirectoryKeys == null || storageDirectoryKeys.isEmpty()) {
                return cb.disjunction();
            }

            List<Predicate> predicates = storageDirectoryKeys.stream()
                    .map(directoryKey -> cb.like(
                            root.get(MediaEntity_.storageKey),
                            directoryKey + "/%"))
                    .toList();
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
