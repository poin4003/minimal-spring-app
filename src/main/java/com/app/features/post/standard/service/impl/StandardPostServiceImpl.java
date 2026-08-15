package com.app.features.post.standard.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.service.MediaService;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.enums.PostMediaRole;
import com.app.features.post.enums.PostType;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.schema.payload.CreateStandardPostPayload;
import com.app.features.post.schema.payload.UpdateStandardPostPayload;
import com.app.features.post.service.PostMediaService;
import com.app.features.post.service.PostService;
import com.app.features.post.standard.entity.StandardPostEntity;
import com.app.features.post.standard.mapper.StandardPostResultMapper;
import com.app.features.post.standard.repository.StandardPostRepository;
import com.app.features.post.standard.repository.spec.StandardPostSpecification;
import com.app.features.post.standard.schema.filter.OwnerStandardPostFilterCriteria;
import com.app.features.post.standard.schema.filter.PublicStandardPostFilterCriteria;
import com.app.features.post.standard.schema.result.OwnerStandardPostResult;
import com.app.features.post.standard.schema.result.PublicStandardPostResult;
import com.app.features.post.standard.service.StandardPostService;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.service.ProfileService;
import com.app.features.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StandardPostServiceImpl implements StandardPostService {

    private final UserService userSvc;
    private final ProfileService profileSvc;
    private final PostService postSvc;
    private final StandardPostRepository standardPostRepo;
    private final MediaService mediaSvc;
    private final PostMediaService postMediaSvc;
    private final StandardPostResultMapper standardPostMapper;

    @Override
    @Transactional
    public OwnerStandardPostResult createStandardPost(
            UUID authorId,
            CreateStandardPostPayload payload) {
        UserBaseEntity author = userSvc.requireUser(authorId);
        UserInfoEntity authorInfo = profileSvc.requireProfile(authorId);
        List<MediaEntity> orderedMedia = mediaSvc.requireOwnedActiveMedia(
                payload.getMediaIds(),
                authorId);

        PostEntity post = postSvc.createDraftPost(author, PostType.STANDARD);

        StandardPostEntity standardPost = new StandardPostEntity();
        standardPost.setPost(post);
        standardPost.setContent(payload.getContent());
        standardPost = standardPostRepo.save(standardPost);

        List<PostMediaEntity> attachments = postMediaSvc.createAttachments(
                post, PostMediaRole.CONTENT, orderedMedia);

        return standardPostMapper.toOwnerResult(
                standardPost,
                authorInfo,
                attachments);
    }

    @Override
    @Transactional
    public void submitOwnedPostForReview(
            UUID postId,
            UUID ownerId) {
        PostEntity post = postSvc.requireOwnedPostForUpdate(postId, ownerId);
        StandardPostEntity standardPost = requireStandardPost(postId);
        List<PostMediaEntity> attachments = postMediaSvc.requirePublishableMedia(post);

        if (!StringUtils.hasText(standardPost.getContent())
                && attachments.isEmpty()) {
            throw ExceptionFactory.invalidParam(
                    "error.post.contentRequired",
                    postId);
        }

        postSvc.submitForReview(post);
    }

    @Override
    @Transactional
    public OwnerStandardPostResult updateOwnedStandardPost(
            UUID postId,
            UUID ownerId,
            UpdateStandardPostPayload payload) {
        List<MediaEntity> orderedMedia = mediaSvc.requireOwnedActiveMedia(
                payload.getMediaIds(),
                ownerId);
        PostEntity post = postSvc.prepareOwnedPostForUpdate(postId, ownerId);
        StandardPostEntity standardPost = requireStandardPost(postId);

        standardPost.setContent(payload.getContent());

        List<PostMediaEntity> attachments = postMediaSvc.replaceAttachments(
                post,
                PostMediaRole.CONTENT,
                orderedMedia);

        return standardPostMapper.toOwnerResult(
                standardPost,
                profileSvc.requireProfile(ownerId),
                attachments);
    }

    @Override
    public PublicStandardPostResult getPublishedPost(UUID postId) {
        StandardPostEntity standardPost = requireStandardPost(postId);

        PostEntity post = standardPost.getPost();

        if (post.getLifecycleStatus() != PostLifecycleStatus.ACTIVE
                || post.getModerationStatus() != PostModerationStatus.PUBLISHED) {
            throw ExceptionFactory.notFound("error.post.notFound", postId);
        }

        UserInfoEntity authorInfo = profileSvc.requireProfile(post.getAuthor().getId());

        return standardPostMapper.toPublicResult(
                standardPost,
                authorInfo,
                postMediaSvc.findAttachments(postId, PostMediaRole.CONTENT));
    }

    @Override
    public OwnerStandardPostResult getOwnerPost(UUID postId, UUID ownerId) {
        StandardPostEntity standardPost = requireOwnedStandardPost(postId, ownerId);

        return standardPostMapper.toOwnerResult(
                standardPost,
                profileSvc.requireProfile(ownerId),
                postMediaSvc.findAttachments(postId, PostMediaRole.CONTENT));
    }

    @Override
    public StandardPostEntity requireStandardPost(UUID postId) {
        return standardPostRepo.findDetailByPostId(postId)
                .orElseThrow(() -> ExceptionFactory.notFound("error.post.notFound", postId));
    }

    private StandardPostEntity requireOwnedStandardPost(UUID postId, UUID ownerId) {
        StandardPostEntity standardPost = requireStandardPost(postId);

        postSvc.requireOwnedPost(standardPost.getPost(), ownerId);

        return standardPost;
    }

    private OwnerStandardPostResult toOwnerResult(
            StandardPostEntity standardPost,
            UUID ownerId) {
        UUID postId = standardPost.getPostId();

        return standardPostMapper.toOwnerResult(
                standardPost,
                profileSvc.requireProfile(ownerId),
                postMediaSvc.findAttachments(postId, PostMediaRole.CONTENT));
    }

    @Override
    public Page<PublicStandardPostResult> getPublishedPosts(
            PublicStandardPostFilterCriteria criteria,
            Pageable pageable) {
        Page<StandardPostEntity> entityPage = standardPostRepo.findAll(
                StandardPostSpecification.published(criteria),
                pageable);

        Map<UUID, UserInfoEntity> profilesByAuthorId = profileSvc
                .requireProfiles(entityPage.getContent().stream()
                        .map(standardPost -> standardPost
                                .getPost()
                                .getAuthor()
                                .getId())
                        .toList());
        Map<UUID, List<PostMediaEntity>> attachmentsByPostId = postMediaSvc
                .findAttachmentsByPostId(
                        entityPage.getContent().stream()
                                .map(standardPost -> standardPost.getPostId())
                                .toList(),
                        PostMediaRole.CONTENT);

        return entityPage.map(standardPost -> {
            PostEntity post = standardPost.getPost();
            return standardPostMapper.toPublicResult(
                    standardPost,
                    profileSvc.requireProfile(
                            profilesByAuthorId,
                            post.getAuthor().getId()),
                    attachmentsByPostId.getOrDefault(post.getId(), List.of()));
        });
    }

    @Override
    public Page<OwnerStandardPostResult> getOwnedPosts(
            UUID ownerId,
            OwnerStandardPostFilterCriteria criteria,
            Pageable pageable) {
        Page<StandardPostEntity> entityPage = standardPostRepo.findAll(
                StandardPostSpecification.ownedBy(ownerId, criteria),
                pageable);

        Map<UUID, UserInfoEntity> profilesByAuthorId = profileSvc
                .requireProfiles(entityPage.getContent().stream()
                        .map(standardPost -> standardPost
                                .getPost()
                                .getAuthor()
                                .getId())
                        .toList());
        Map<UUID, List<PostMediaEntity>> attachmentsByPostId = postMediaSvc
                .findAttachmentsByPostId(
                        entityPage.getContent().stream()
                                .map(standardPost -> standardPost.getPostId())
                                .toList(),
                        PostMediaRole.CONTENT);

        return entityPage.map(standardPost -> {
            PostEntity post = standardPost.getPost();
            return standardPostMapper.toOwnerResult(
                    standardPost,
                    profileSvc.requireProfile(
                            profilesByAuthorId,
                            post.getAuthor().getId()),
                    attachmentsByPostId.getOrDefault(post.getId(), List.of()));
        });
    }

}
