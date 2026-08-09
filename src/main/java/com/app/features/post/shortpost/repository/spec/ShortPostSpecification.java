package com.app.features.post.shortpost.repository.spec;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.app.features.post.repository.spec.PostDetailSpecification;
import com.app.features.post.shortpost.entity.ShortPostEntity;
import com.app.features.post.shortpost.entity.ShortPostEntity_;
import com.app.features.post.shortpost.schema.filter.OwnerShortPostFilterCriteria;
import com.app.features.post.shortpost.schema.filter.PublicShortPostFilterCriteria;

public class ShortPostSpecification {

    public static Specification<ShortPostEntity> published(
            PublicShortPostFilterCriteria criteria) {
        return PostDetailSpecification.published(
                ShortPostEntity_.post,
                criteria.getAuthorId());
    }

    public static Specification<ShortPostEntity> ownedBy(
            UUID ownerId,
            OwnerShortPostFilterCriteria criteria) {
        return PostDetailSpecification.ownedBy(
                ShortPostEntity_.post,
                ownerId,
                criteria.getLifecycleStatus(),
                criteria.getModerationStatus());
    }
}
