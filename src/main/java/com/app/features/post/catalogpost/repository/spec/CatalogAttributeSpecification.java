package com.app.features.post.catalogpost.repository.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.app.features.post.catalogpost.entity.CatalogAttributeEntity;
import com.app.features.post.catalogpost.entity.CatalogAttributeEntity_;
import com.app.features.post.catalogpost.schema.filter.CatalogAttributeFilterCriteria;

import jakarta.persistence.criteria.Predicate;

public final class CatalogAttributeSpecification {

    private CatalogAttributeSpecification() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<CatalogAttributeEntity> withFilter(
            CatalogAttributeFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.getText())) {
                String pattern = "%"
                        + criteria.getText().trim().toLowerCase(Locale.ROOT)
                        + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get(
                                CatalogAttributeEntity_.key)), pattern),
                        cb.like(cb.lower(root.get(
                                CatalogAttributeEntity_.name)), pattern)));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(
                        root.get(CatalogAttributeEntity_.status),
                        criteria.getStatus()));
            }

            if (criteria.getValueType() != null) {
                predicates.add(cb.equal(
                        root.get(CatalogAttributeEntity_.valueType),
                        criteria.getValueType()));
            }

            if (criteria.getSearchable() != null) {
                predicates.add(cb.equal(
                        root.get(CatalogAttributeEntity_.searchable),
                        criteria.getSearchable()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
