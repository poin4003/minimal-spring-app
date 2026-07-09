package com.app.features.rbac.repository.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.app.features.rbac.entity.PermissionEntity;
import com.app.features.rbac.entity.PermissionEntity_;
import com.app.features.rbac.entity.RoleEntity;
import com.app.features.rbac.entity.RoleEntity_;
import com.app.features.rbac.filter.PermissionFilterCriteria;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

public class PermissionSpecification {

    public static Specification<PermissionEntity> withFilter(PermissionFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getRoleId() != null) {
                Join<PermissionEntity, RoleEntity> roleJoin = root.join(PermissionEntity_.roles);
                predicates.add(cb.equal(roleJoin.get(RoleEntity_.id), criteria.getRoleId()));
            }

            if (query != null) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
