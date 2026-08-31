package com.app.features.ai.search.web.support;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.features.ai.search.schema.model.PostSearchItem;
import com.app.features.ai.search.web.view.PostSearchItemView;
import com.app.features.ai.search.web.view.PostSearchMediaView;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.support.MediaUrlResolver;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.enums.PostType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostSearchItemViewFactory {

    private final AppProperties appProperties;
    private final MediaUrlResolver mediaUrlResolver;

    public PostSearchItemView build(
            PostSearchItem item,
            List<PostMediaEntity> attachments) {
        return PostSearchItemView.builder()
                .content(item.content())
                .detailPath(buildDetailPath(item))
                .relevancePercent(resolveRelevancePercent(item.score()))
                .media(attachments.stream()
                        .map(attachment -> buildMedia(
                                attachment.getMedia()))
                        .toList())
                .build();
    }

    private PostSearchMediaView buildMedia(MediaEntity media) {
        String previewUrl = mediaUrlResolver.resolveThumbnailUrl(media);
        if (previewUrl == null && media.getKind() == MediaKind.IMAGE) {
            previewUrl = mediaUrlResolver.resolveContentUrl(media);
        }

        return PostSearchMediaView.builder()
                .kind(media.getKind())
                .previewUrl(previewUrl)
                .originalWidth(media.getOriginalWidth())
                .originalHeight(media.getOriginalHeight())
                .build();
    }

    private int resolveRelevancePercent(float score) {
        return Math.clamp(Math.round(score * 100), 0, 100);
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
