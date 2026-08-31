package com.app.features.ai.search.web.support;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.core.i18n.AppMessageResolver;
import com.app.features.ai.search.schema.model.PostSearchItem;
import com.app.features.ai.search.web.view.PostSearchItemView;
import com.app.features.post.enums.PostType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostSearchItemViewFactory {

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;

    public PostSearchItemView build(PostSearchItem item) {
        return PostSearchItemView.builder()
                .rank(item.rank())
                .postTypeLabel(resolvePostTypeLabel(item.postType()))
                .content(item.content())
                .detailPath(buildDetailPath(item))
                .build();
    }

    private String resolvePostTypeLabel(PostType postType) {
        String messageCode = switch (postType) {
            case STANDARD -> "post.type.standard";
            case SHORT -> "post.type.short";
            case VIDEO -> "post.type.video";
            case PRODUCT -> "post.type.product";
            case WIKI -> "post.type.wiki";
            case BLOG -> "post.type.blog";
        };
        return messageResolver.get(messageCode);
    }

    private String buildDetailPath(PostSearchItem item) {
        String basePath = switch (item.postType()) {
            case STANDARD -> appProperties.getUi().getFeedPath();
            case SHORT -> appProperties.getUi().getShortsPath();
            case VIDEO -> appProperties.getUi().getVideosPath();
            case PRODUCT, WIKI, BLOG -> null;
        };
        if (basePath == null) {
            return null;
        }

        return UriComponentsBuilder.fromPath(basePath)
                .pathSegment(item.postId().toString())
                .build()
                .encode()
                .toUriString();
    }
}
