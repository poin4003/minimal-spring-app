package com.app.features.post.catalogpost.repository.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.app.features.post.catalogpost.entity.CatalogCategoryEntity;
import com.app.features.post.catalogpost.entity.CatalogCategoryEntity_;
import com.app.features.post.catalogpost.schema.filter.CatalogCategoryFilterCriteria;

import jakarta.persistence.criteria.Predicate;

public final class CatalogCategorySpecification {

    private CatalogCategorySpecification() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<CatalogCategoryEntity> withFilter(
            CatalogCategoryFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.getText())) {
                String pattern = "%"
                        + criteria.getText().trim().toLowerCase(Locale.ROOT)
                        + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get(
                                CatalogCategoryEntity_.name)), pattern),
                        cb.like(cb.lower(root.get(
                                CatalogCategoryEntity_.slug)), pattern)));
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(
                        root.get(CatalogCategoryEntity_.status),
                        criteria.getStatus()));
            }

            if (criteria.getParentId() != null) {
                predicates.add(cb.equal(
                        root.get(CatalogCategoryEntity_.parent)
                                .get(CatalogCategoryEntity_.id),
                        criteria.getParentId()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
