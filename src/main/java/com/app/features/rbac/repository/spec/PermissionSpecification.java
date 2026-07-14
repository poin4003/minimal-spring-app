package com.app.features.rbac.repository.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.app.features.rbac.entity.PermissionEntity;
import com.app.features.rbac.entity.PermissionEntity_;
import com.app.features.rbac.entity.RoleEntity;
import com.app.features.rbac.entity.RoleEntity_;
import com.app.features.rbac.schema.filter.PermissionFilterCriteria;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class PermissionSpecification {

    public static Specification<PermissionEntity> withFilter(PermissionFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getRoleId() != null) {
                Join<PermissionEntity, RoleEntity> roleJoin = root.join(PermissionEntity_.roles);
                predicates.add(cb.equal(roleJoin.get(RoleEntity_.id), criteria.getRoleId()));
            }

            if (criteria.getExcludeRoleId() != null && query != null) {
                Subquery<java.util.UUID> subquery = query.subquery(java.util.UUID.class);
                Root<RoleEntity> roleRoot = subquery.from(RoleEntity.class);
                Join<RoleEntity, PermissionEntity> permissionJoin = roleRoot.join(RoleEntity_.permissions);

                subquery.select(permissionJoin.get(PermissionEntity_.id))
                        .where(cb.equal(roleRoot.get(RoleEntity_.id), criteria.getExcludeRoleId()));

                predicates.add(cb.not(root.get(PermissionEntity_.id).in(subquery)));
            }

            if (query != null) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
