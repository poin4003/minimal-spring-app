package com.app.features.ai.search.web.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.core.i18n.AppMessageResolver;
import com.app.features.ai.search.schema.model.PostSearchItem;
import com.app.features.ai.search.schema.model.PostSearchRequest;
import com.app.features.ai.search.schema.model.PostSearchResult;
import com.app.features.ai.search.service.PostSearchService;
import com.app.features.ai.search.web.service.PostSearchResultsService;
import com.app.features.ai.search.web.support.PostSearchItemViewFactory;
import com.app.features.ai.search.web.view.PostSearchItemView;
import com.app.features.ai.search.web.view.PostSearchResultsView;
import com.app.features.ai.search.web.view.PostSearchSectionView;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.enums.PostMediaRole;
import com.app.features.post.enums.PostType;
import com.app.features.post.service.PostMediaService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class PostSearchResultsServiceImpl
        implements PostSearchResultsService {

    private final AppProperties appProperties;
    private final PostSearchService postSearchSvc;
    private final PostMediaService postMediaSvc;
    private final AppMessageResolver messageResolver;
    private final PostSearchItemViewFactory postSearchItemViewFactory;

    @Override
    public PostSearchResultsView search(String query) {
        PostSearchResult result = postSearchSvc.search(
                new PostSearchRequest(
                        query,
                        null,
                        appProperties.getAi().getSearch()
                                .getDefaultLimit()));
        Map<UUID, List<PostMediaEntity>> attachmentsByPostId =
                loadAttachments(result);
        Map<PostType, List<PostSearchItemView>> itemsByPostType =
                buildItemsByPostType(result, attachmentsByPostId);
        List<PostSearchSectionView> sections = List.of(
                        PostType.STANDARD,
                        PostType.SHORT,
                        PostType.VIDEO)
                .stream()
                .filter(postType -> itemsByPostType.containsKey(postType))
                .map(postType -> buildSection(
                        postType,
                        itemsByPostType.get(postType)))
                .toList();

        return PostSearchResultsView.builder()
                .retrievalAvailability(result.availability())
                .sections(sections)
                .build();
    }

    private Map<PostType, List<PostSearchItemView>> buildItemsByPostType(
            PostSearchResult result,
            Map<UUID, List<PostMediaEntity>> attachmentsByPostId) {
        Map<PostType, List<PostSearchItemView>> itemsByPostType =
                new EnumMap<>(PostType.class);

        for (PostSearchItem item : result.items()) {
            if (!isSupportedSection(item.postType())) {
                continue;
            }

            PostSearchItemView itemView = postSearchItemViewFactory.build(
                    item,
                    attachmentsByPostId.getOrDefault(
                            item.postId(),
                            List.of()));
            itemsByPostType.computeIfAbsent(
                    item.postType(),
                    ignoredPostType -> new ArrayList<>())
                    .add(itemView);
        }

        return itemsByPostType;
    }

    private Map<UUID, List<PostMediaEntity>> loadAttachments(
            PostSearchResult result) {
        Collection<UUID> postIds = result.items().stream()
                .filter(item -> isSupportedSection(item.postType()))
                .map(item -> item.postId())
                .toList();
        if (postIds.isEmpty()) {
            return Map.of();
        }

        return postMediaSvc.findAttachmentsByPostId(
                postIds,
                PostMediaRole.CONTENT);
    }

    private PostSearchSectionView buildSection(
            PostType postType,
            List<PostSearchItemView> items) {
        return PostSearchSectionView.builder()
                .postType(postType)
                .title(messageResolver.get(resolveSectionTitle(postType)))
                .items(List.copyOf(items))
                .build();
    }

    private String resolveSectionTitle(PostType postType) {
        return switch (postType) {
            case STANDARD -> "ai.search.section.standard";
            case SHORT -> "ai.search.section.short";
            case VIDEO -> "ai.search.section.video";
            case PRODUCT, WIKI, BLOG -> throw new IllegalArgumentException(
                    "Unsupported search section: " + postType);
        };
    }

    private boolean isSupportedSection(PostType postType) {
        return postType == PostType.STANDARD
                || postType == PostType.SHORT
                || postType == PostType.VIDEO;
    }
}
