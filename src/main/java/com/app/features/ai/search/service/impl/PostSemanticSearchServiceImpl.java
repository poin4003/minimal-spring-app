package com.app.features.ai.search.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.features.ai.embedding.service.AiEmbeddingClient;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.search.exceptions.AiSearchRuntimeException;
import com.app.features.ai.search.schema.model.PostSemanticSearchResult;
import com.app.features.ai.search.schema.model.PostVectorSearchHit;
import com.app.features.ai.search.service.PostSemanticSearchService;
import com.app.features.ai.search.service.PostVectorIndex;
import com.app.features.ai.search.support.AiSearchCapability;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.enums.PostType;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.repository.PostRepository;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class PostSemanticSearchServiceImpl
        implements PostSemanticSearchService {

    private final AppProperties appProperties;
    private final AiSearchCapability aiSearchCapability;
    private final ObjectProvider<AiEmbeddingClient>
            aiEmbeddingClientProvider;
    private final ObjectProvider<PostVectorIndex> postVectorIndexProvider;
    private final PostRepository postRepo;

    @Override
    public PostSemanticSearchResult search(String query) {
        return search(
                query,
                appProperties.getAi().getSearch().getDefaultLimit());
    }

    @Override
    public PostSemanticSearchResult search(String query, int limit) {
        AiAvailability availability = aiSearchCapability
                .resolveAvailability();
        if (availability != AiAvailability.READY) {
            return PostSemanticSearchResult.unavailable(availability);
        }

        int maxLimit = appProperties.getAi().getSearch().getMaxLimit();
        int effectiveLimit = Math.min(limit, maxLimit);
        int candidateLimit = Math.min(maxLimit, effectiveLimit * 2);
        float[] queryVector = requireEmbeddingClient().embedQuery(
                query.trim());
        List<PostVectorSearchHit> candidates = requirePostVectorIndex()
                .search(queryVector, candidateLimit);

        return PostSemanticSearchResult.ready(
                keepCurrentlyVisiblePosts(candidates, effectiveLimit));
    }

    private List<PostVectorSearchHit> keepCurrentlyVisiblePosts(
            List<PostVectorSearchHit> candidates,
            int limit) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<UUID> postIds = candidates.stream()
                .map(candidate -> candidate.postId())
                .toList();
        Map<UUID, PostType> visiblePostTypes = postRepo
                .findAllByIdInAndLifecycleStatusAndModerationStatus(
                        postIds,
                        PostLifecycleStatus.ACTIVE,
                        PostModerationStatus.PUBLISHED)
                .stream()
                .collect(Collectors.toMap(
                        post -> post.getId(),
                        post -> post.getType(),
                        (existingPostType, ignoredPostType) ->
                                existingPostType));

        return candidates.stream()
                .filter(candidate -> candidate.postType()
                        == visiblePostTypes.get(candidate.postId()))
                .limit(limit)
                .toList();
    }

    private AiEmbeddingClient requireEmbeddingClient() {
        AiEmbeddingClient embeddingClient = aiEmbeddingClientProvider
                .getIfAvailable();
        if (embeddingClient == null) {
            throw new AiSearchRuntimeException(
                    "AI embedding runtime is unavailable for semantic search.");
        }
        return embeddingClient;
    }

    private PostVectorIndex requirePostVectorIndex() {
        PostVectorIndex postVectorIndex = postVectorIndexProvider
                .getIfAvailable();
        if (postVectorIndex == null) {
            throw new AiSearchRuntimeException(
                    "Post vector index is unavailable for semantic search.");
        }
        return postVectorIndex;
    }
}
