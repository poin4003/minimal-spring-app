package com.app.features.post.videopost.repository.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.app.features.post.entity.PostEntity_;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.videopost.entity.VideoPostEntity_;
import com.app.features.post.videopost.entity.VideoSeriesEntity;
import com.app.features.post.videopost.entity.VideoSeriesEntity_;
import com.app.features.post.videopost.entity.VideoSeriesItemEntity;
import com.app.features.post.videopost.entity.VideoSeriesItemEntity_;
import com.app.features.post.videopost.schema.filter.VideoSeriesFilterCriteria;
import com.app.features.user.entity.UserBaseEntity_;

import jakarta.persistence.criteria.Predicate;

public final class VideoSeriesSpecification {

    private VideoSeriesSpecification() {
    }

    public static Specification<VideoSeriesEntity> published(
            VideoSeriesFilterCriteria criteria) {
        return filter(criteria, criteria.getOwnerId())
                .and(hasPublishedVideo());
    }

    public static Specification<VideoSeriesEntity> ownedBy(
            UUID ownerId,
            VideoSeriesFilterCriteria criteria) {
        return filter(criteria, ownerId);
    }

    private static Specification<VideoSeriesEntity> filter(
            VideoSeriesFilterCriteria criteria,
            UUID ownerId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (ownerId != null) {
                predicates.add(cb.equal(
                        root.get(VideoSeriesEntity_.owner)
                                .get(UserBaseEntity_.id),
                        ownerId));
            }
            if (StringUtils.hasText(criteria.getTitle())) {
                predicates.add(cb.like(
                        cb.lower(root.get(VideoSeriesEntity_.title)),
                        "%" + criteria.getTitle().trim().toLowerCase(Locale.ROOT) + "%"));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Specification<VideoSeriesEntity> hasPublishedVideo() {
        return (root, query, cb) -> {
            var subquery = query.subquery(UUID.class);
            var item = subquery.from(VideoSeriesItemEntity.class);
            var post = item.get(VideoSeriesItemEntity_.videoPost)
                    .get(VideoPostEntity_.post);

            subquery.select(item.get(VideoSeriesItemEntity_.id));
            subquery.where(
                    cb.equal(
                            item.get(VideoSeriesItemEntity_.series)
                                    .get(VideoSeriesEntity_.id),
                            root.get(VideoSeriesEntity_.id)),
                    cb.equal(
                            post.get(PostEntity_.lifecycleStatus),
                            PostLifecycleStatus.ACTIVE),
                    cb.equal(
                            post.get(PostEntity_.moderationStatus),
                            PostModerationStatus.PUBLISHED));

            return cb.exists(subquery);
        };
    }
}
