package com.app.features.ai.search.support;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.app.features.ai.search.exceptions.AiSearchRuntimeException;
import com.app.features.ai.search.schema.model.PostSearchCandidate;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.repository.PostRepository;
import com.app.features.post.shortpost.repository.ShortPostRepository;
import com.app.features.post.standard.repository.StandardPostRepository;
import com.app.features.post.videopost.repository.VideoPostRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostSearchCandidateFactory {

    private final PostRepository postRepo;
    private final StandardPostRepository standardPostRepo;
    private final ShortPostRepository shortPostRepo;
    private final VideoPostRepository videoPostRepo;

    @Transactional(readOnly = true)
    public Optional<PostSearchCandidate> findIndexable(UUID postId) {
        Optional<PostEntity> postOptional = postRepo.findById(postId);
        if (postOptional.isEmpty()) {
            return Optional.empty();
        }

        PostEntity post = postOptional.get();
        if (post.getLifecycleStatus() != PostLifecycleStatus.ACTIVE
                || post.getModerationStatus()
                != PostModerationStatus.PUBLISHED) {
            return Optional.empty();
        }

        String content = switch (post.getType()) {
            case STANDARD -> standardPostRepo.findById(postId)
                    .map(standardPost -> standardPost.getContent())
                    .orElseThrow(() -> missingPostDetail(post));
            case SHORT -> shortPostRepo.findById(postId)
                    .map(shortPost -> shortPost.getCaption())
                    .orElseThrow(() -> missingPostDetail(post));
            case VIDEO -> videoPostRepo.findById(postId)
                    .map(videoPost -> joinText(
                            videoPost.getTitle(),
                            videoPost.getDescription()))
                    .orElseThrow(() -> missingPostDetail(post));
            case PRODUCT, WIKI, BLOG -> null;
        };
        if (!StringUtils.hasText(content)) {
            return Optional.empty();
        }

        return Optional.of(new PostSearchCandidate(
                post.getId(),
                post.getType(),
                post.getUpdatedAt(),
                content));
    }

    private String joinText(String... values) {
        return Stream.of(values)
                .filter(value -> StringUtils.hasText(value))
                .map(value -> value.trim())
                .collect(Collectors.joining("\n\n"));
    }

    private AiSearchRuntimeException missingPostDetail(PostEntity post) {
        return new AiSearchRuntimeException(
                "Published post ["
                        + post.getId()
                        + "] is missing its ["
                        + post.getType()
                        + "] detail record.");
    }
}
