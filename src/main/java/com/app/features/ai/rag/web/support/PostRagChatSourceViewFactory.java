package com.app.features.ai.rag.web.support;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.core.i18n.AppMessageResolver;
import com.app.features.ai.rag.schema.model.PostRagSource;
import com.app.features.ai.rag.web.view.PostRagChatSourceView;
import com.app.features.post.enums.PostType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostRagChatSourceViewFactory {

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;

    public PostRagChatSourceView build(PostRagSource source) {
        return PostRagChatSourceView.builder()
                .rank(source.rank())
                .postTypeLabel(resolvePostTypeLabel(source.postType()))
                .relevancePercent(toRelevancePercent(source.score()))
                .content(source.content())
                .detailPath(buildDetailPath(source))
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

    private String buildDetailPath(PostRagSource source) {
        String basePath = switch (source.postType()) {
            case STANDARD -> appProperties.getUi().getFeedPath();
            case SHORT -> appProperties.getUi().getShortsPath();
            case VIDEO -> appProperties.getUi().getVideosPath();
            case PRODUCT, WIKI, BLOG -> null;
        };
        if (basePath == null) {
            return null;
        }

        return UriComponentsBuilder.fromPath(basePath)
                .pathSegment(source.postId().toString())
                .build()
                .encode()
                .toUriString();
    }

    private int toRelevancePercent(float score) {
        float boundedScore = Math.max(0.0f, Math.min(1.0f, score));
        return Math.round(boundedScore * 100.0f);
    }
}
