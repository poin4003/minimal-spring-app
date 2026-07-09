package com.app.features.rbac.repository.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.app.features.rbac.entity.RoleEntity;
import com.app.features.rbac.entity.RoleEntity_;
import com.app.features.rbac.filter.RoleFilterCriteria;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.entity.UserBaseEntity_;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

public class RoleSpecification {

    public static Specification<RoleEntity> withFilter(RoleFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getUserId() != null) {
                Join<RoleEntity, UserBaseEntity> userJoin = root.join(RoleEntity_.users);
                predicates.add(cb.equal(userJoin.get(UserBaseEntity_.id), criteria.getUserId()));
            }

            if (query != null) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
