package com.app.features.post.standard.repository.spec;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.app.features.post.repository.spec.PostDetailSpecification;
import com.app.features.post.standard.entity.StandardPostEntity;
import com.app.features.post.standard.entity.StandardPostEntity_;
import com.app.features.post.standard.schema.filter.OwnerStandardPostFilterCriteria;
import com.app.features.post.standard.schema.filter.PublicStandardPostFilterCriteria;

public class StandardPostSpecification {

    public static Specification<StandardPostEntity> published(
            PublicStandardPostFilterCriteria criteria) {
        return PostDetailSpecification.published(
                StandardPostEntity_.post,
                criteria.getAuthorId());
    }

    public static Specification<StandardPostEntity> ownedBy(
            UUID ownerId,
            OwnerStandardPostFilterCriteria criteria) {
        return PostDetailSpecification.ownedBy(
                StandardPostEntity_.post,
                ownerId,
                criteria.getLifecycleStatus(),
                criteria.getModerationStatus());
    }
}
