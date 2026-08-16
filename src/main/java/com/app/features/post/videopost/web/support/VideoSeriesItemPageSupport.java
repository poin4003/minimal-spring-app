package com.app.features.post.videopost.web.support;

import java.util.function.Function;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.features.post.videopost.entity.VideoSeriesItemEntity_;
import com.app.features.post.videopost.web.view.VideoSeriesItemSortView;

@Component
public class VideoSeriesItemPageSupport {

    private static final UiPageDefaults DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(20)
                    .sortBy(VideoSeriesItemEntity_.POSITION)
                    .sortDirection(Sort.Direction.ASC)
                    .build();

    public UiPageQuery normalize(UiPageQuery query) {
        UiPageQuery normalized = query.applyDefaults(DEFAULTS);
        normalized.setSortBy(VideoSeriesItemEntity_.POSITION);
        normalized.setSortDirection(
                normalized.getSortDirection() == Sort.Direction.DESC
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC);
        return normalized;
    }

    public VideoSeriesItemSortView buildSort(
            UiPageQuery query,
            Function<UiPageQuery, String> pathFactory) {
        UiPageQuery normalized = normalize(query);
        UiPageQuery reversed = normalized.copy();
        reversed.setPage(0);
        reversed.setSortDirection(
                normalized.getSortDirection() == Sort.Direction.ASC
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC);

        return VideoSeriesItemSortView.builder()
                .ascending(normalized.getSortDirection() == Sort.Direction.ASC)
                .togglePath(pathFactory.apply(reversed))
                .build();
    }

    public UiPageDefaults getDefaults() {
        return DEFAULTS;
    }
}
