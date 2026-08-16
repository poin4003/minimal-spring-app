package com.app.features.post.videopost.repository.spec;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.app.features.post.repository.spec.PostDetailSpecification;
import com.app.features.post.videopost.entity.VideoPostEntity;
import com.app.features.post.videopost.entity.VideoPostEntity_;
import com.app.features.post.videopost.entity.VideoSeriesEntity_;
import com.app.features.post.videopost.entity.VideoSeriesItemEntity;
import com.app.features.post.videopost.entity.VideoSeriesItemEntity_;
import com.app.features.post.videopost.enums.VideoSeriesLifecycleStatus;
import com.app.features.post.videopost.schema.filter.OwnerVideoPostFilterCriteria;
import com.app.features.post.videopost.schema.filter.PublicVideoPostFilterCriteria;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public final class VideoPostSpecification {

    private VideoPostSpecification() {
    }

    public static Specification<VideoPostEntity> published(
            PublicVideoPostFilterCriteria criteria) {
        Specification<VideoPostEntity> specification =
                PostDetailSpecification.published(
                        VideoPostEntity_.post,
                        criteria.getAuthorId())
                        .and(titleContains(criteria.getTitle()));

        if (criteria.getSeriesId() != null) {
            return specification.and(inSeries(criteria.getSeriesId()));
        }
        return specification.and(notInAnySeries());
    }

    public static Specification<VideoPostEntity> ownedBy(
            UUID ownerId,
            OwnerVideoPostFilterCriteria criteria) {
        Specification<VideoPostEntity> specification =
                PostDetailSpecification.ownedBy(
                VideoPostEntity_.post,
                ownerId,
                criteria.getLifecycleStatus(),
                criteria.getModerationStatus())
                .and(titleContains(criteria.getTitle()));

        if (criteria.getSeriesId() != null) {
            return specification.and(inSeries(criteria.getSeriesId()));
        }
        return specification.and(notInAnySeries());
    }

    private static Specification<VideoPostEntity> titleContains(
            String title) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(title)) {
                return cb.conjunction();
            }
            return cb.like(
                    cb.lower(root.get(VideoPostEntity_.title)),
                    "%" + title.trim().toLowerCase(Locale.ROOT) + "%");
        };
    }

    private static Specification<VideoPostEntity> inSeries(
            UUID seriesId) {
        return (root, query, cb) -> cb.exists(seriesMembership(
                root,
                query.subquery(UUID.class),
                seriesId,
                false,
                cb));
    }

    private static Specification<VideoPostEntity> notInAnySeries() {
        return (root, query, cb) -> cb.not(cb.exists(seriesMembership(
                root,
                query.subquery(UUID.class),
                null,
                true,
                cb)));
    }

    private static Subquery<UUID> seriesMembership(
            Root<VideoPostEntity> videoPost,
            Subquery<UUID> subquery,
            UUID seriesId,
            boolean anySeries,
            jakarta.persistence.criteria.CriteriaBuilder cb) {
        Root<VideoSeriesItemEntity> item = subquery.from(
                VideoSeriesItemEntity.class);
        subquery.select(item.get(VideoSeriesItemEntity_.id));

        var sameVideo = cb.equal(
                item.get(VideoSeriesItemEntity_.videoPost)
                        .get(VideoPostEntity_.postId),
                videoPost.get(VideoPostEntity_.postId));
        var activeSeries = cb.equal(
                item.get(VideoSeriesItemEntity_.series)
                        .get(VideoSeriesEntity_.lifecycleStatus),
                VideoSeriesLifecycleStatus.ACTIVE);
        if (anySeries) {
            return subquery.where(sameVideo, activeSeries);
        }
        return subquery.where(
                sameVideo,
                activeSeries,
                cb.equal(
                        item.get(VideoSeriesItemEntity_.series)
                                .get(VideoSeriesEntity_.id),
                        seriesId));
    }
}
