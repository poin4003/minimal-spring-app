package com.app.features.post.shortpost.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import com.app.features.post.service.PostMediaService;
import com.app.features.post.service.PostService;
import com.app.features.post.shortpost.entity.ShortPostEntity;
import com.app.features.post.shortpost.mapper.ShortPostResultMapper;
import com.app.features.post.shortpost.repository.ShortPostRepository;
import com.app.features.post.shortpost.repository.spec.ShortPostSpecification;
import com.app.features.post.shortpost.schema.filter.OwnerShortPostFilterCriteria;
import com.app.features.post.shortpost.schema.filter.PublicShortPostFilterCriteria;
import com.app.features.post.shortpost.schema.payload.CreateShortPostPayload;
import com.app.features.post.shortpost.schema.payload.UpdateShortPostPayload;
import com.app.features.post.shortpost.schema.result.OwnerShortPostResult;
import com.app.features.post.shortpost.schema.result.PublicShortPostResult;
import com.app.features.post.shortpost.service.ShortPostPolicy;
import com.app.features.post.shortpost.service.ShortPostService;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.service.ProfileService;
import com.app.features.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShortPostServiceImpl implements ShortPostService {

    private final UserService userSvc;
    private final ProfileService profileSvc;
    private final PostService postSvc;
    private final ShortPostRepository shortPostRepo;
    private final MediaService mediaSvc;
    private final PostMediaService postMediaSvc;
    private final ShortPostPolicy shortPostPolicy;
    private final ShortPostResultMapper shortPostMapper;

    @Override
    @Transactional
    public OwnerShortPostResult createShortPost(
            UUID authorId,
            CreateShortPostPayload payload) {
        UserBaseEntity author = userSvc.requireUser(authorId);
        UserInfoEntity authorInfo = profileSvc.requireProfile(authorId);
        MediaEntity media = requireAllowedOwnedMedia(
                payload.getMediaId(),
                authorId);

        PostEntity post = postSvc.createDraftPost(author, PostType.SHORT);

        ShortPostEntity shortPost = new ShortPostEntity();
        shortPost.setPost(post);
        shortPost.setCaption(payload.getCaption());
        shortPost = shortPostRepo.save(shortPost);

        PostMediaEntity attachment = postMediaSvc.createAttachments(
                post,
                PostMediaRole.CONTENT,
                List.of(media)).getFirst();

        return shortPostMapper.toOwnerResult(
                shortPost,
                authorInfo,
                attachment);
    }

    @Override
    @Transactional
    public OwnerShortPostResult updateOwnedShortPost(
            UUID postId,
            UUID ownerId,
            UpdateShortPostPayload payload) {
        MediaEntity media = requireAllowedOwnedMedia(
                payload.getMediaId(),
                ownerId);
        PostEntity post = postSvc.prepareOwnedPostForUpdate(postId, ownerId);
        ShortPostEntity shortPost = requireShortPost(postId);

        shortPost.setCaption(payload.getCaption());

        PostMediaEntity attachment = postMediaSvc.replaceAttachments(
                post,
                PostMediaRole.CONTENT,
                List.of(media)).getFirst();

        return shortPostMapper.toOwnerResult(
                shortPost,
                profileSvc.requireProfile(ownerId),
                attachment);
    }

    @Override
    @Transactional
    public OwnerShortPostResult submitOwnedPostForReview(
            UUID postId,
            UUID ownerId) {
        PostEntity post = postSvc.requireOwnedPostForUpdate(postId, ownerId);
        ShortPostEntity shortPost = requireShortPost(postId);
        List<PostMediaEntity> attachments =
                postMediaSvc.requirePublishableMedia(post);
        PostMediaEntity attachment = requireContentAttachment(
                postId,
                attachments);

        shortPostPolicy.requireAllowedMedia(attachment.getMedia());
        postSvc.submitForReview(post);

        return shortPostMapper.toOwnerResult(
                shortPost,
                profileSvc.requireProfile(ownerId),
                attachment);
    }

    @Override
    @Transactional
    public OwnerShortPostResult archiveOwnedPost(
            UUID postId,
            UUID ownerId) {
        PostEntity post = postSvc.requireOwnedPostForUpdate(postId, ownerId);
        ShortPostEntity shortPost = requireShortPost(postId);

        postSvc.archivePost(post);

        return toOwnerResult(shortPost, ownerId);
    }

    @Override
    @Transactional
    public OwnerShortPostResult restoreArchivedOwnedPost(
            UUID postId,
            UUID ownerId) {
        PostEntity post = postSvc.requireOwnedPostForUpdate(postId, ownerId);
        ShortPostEntity shortPost = requireShortPost(postId);

        postSvc.restoreArchivedPost(post);

        return toOwnerResult(shortPost, ownerId);
    }

    @Override
    @Transactional
    public OwnerShortPostResult restoreDeletedOwnedPost(
            UUID postId,
            UUID ownerId) {
        PostEntity post = postSvc.requireOwnedPostForUpdate(postId, ownerId);
        ShortPostEntity shortPost = requireShortPost(postId);

        postSvc.restoreDeletedPost(post);

        return toOwnerResult(shortPost, ownerId);
    }

    @Override
    @Transactional
    public void deleteOwnedPost(UUID postId, UUID ownerId) {
        PostEntity post = postSvc.requireOwnedPostForUpdate(postId, ownerId);
        requireShortPost(postId);

        postSvc.markPostDeleted(post);
    }

    @Override
    public PublicShortPostResult getPublishedPost(UUID postId) {
        ShortPostEntity shortPost = requireShortPost(postId);
        PostEntity post = shortPost.getPost();

        if (post.getLifecycleStatus() != PostLifecycleStatus.ACTIVE
                || post.getModerationStatus()
                != PostModerationStatus.PUBLISHED) {
            throw ExceptionFactory.notFound("error.post.notFound", postId);
        }

        return shortPostMapper.toPublicResult(
                shortPost,
                profileSvc.requireProfile(post.getAuthor().getId()),
                requireContentAttachment(postId));
    }

    @Override
    public OwnerShortPostResult getOwnerPost(UUID postId, UUID ownerId) {
        ShortPostEntity shortPost = requireOwnedShortPost(postId, ownerId);

        return shortPostMapper.toOwnerResult(
                shortPost,
                profileSvc.requireProfile(ownerId),
                requireContentAttachment(postId));
    }

    @Override
    public Page<PublicShortPostResult> getPublishedPosts(
            PublicShortPostFilterCriteria criteria,
            Pageable pageable) {
        Page<ShortPostEntity> entityPage = shortPostRepo.findAll(
                ShortPostSpecification.published(criteria),
                pageable);
        Map<UUID, UserInfoEntity> profilesByAuthorId = loadProfilesByAuthorId(
                entityPage.getContent());
        Map<UUID, List<PostMediaEntity>> attachmentsByPostId =
                loadAttachmentsByPostId(entityPage.getContent());

        return entityPage.map(shortPost -> {
            PostEntity post = shortPost.getPost();
            return shortPostMapper.toPublicResult(
                    shortPost,
                    requireLoadedProfile(
                            profilesByAuthorId,
                            post.getAuthor().getId()),
                    requireContentAttachment(
                            post.getId(),
                            attachmentsByPostId.getOrDefault(
                                    post.getId(),
                                    List.of())));
        });
    }

    @Override
    public Page<OwnerShortPostResult> getOwnedPosts(
            UUID ownerId,
            OwnerShortPostFilterCriteria criteria,
            Pageable pageable) {
        Page<ShortPostEntity> entityPage = shortPostRepo.findAll(
                ShortPostSpecification.ownedBy(ownerId, criteria),
                pageable);
        Map<UUID, UserInfoEntity> profilesByAuthorId = loadProfilesByAuthorId(
                entityPage.getContent());
        Map<UUID, List<PostMediaEntity>> attachmentsByPostId =
                loadAttachmentsByPostId(entityPage.getContent());

        return entityPage.map(shortPost -> {
            PostEntity post = shortPost.getPost();
            return shortPostMapper.toOwnerResult(
                    shortPost,
                    requireLoadedProfile(
                            profilesByAuthorId,
                            post.getAuthor().getId()),
                    requireContentAttachment(
                            post.getId(),
                            attachmentsByPostId.getOrDefault(
                                    post.getId(),
                                    List.of())));
        });
    }

    @Override
    public ShortPostEntity requireShortPost(UUID postId) {
        return shortPostRepo.findDetailByPostId(postId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.post.notFound",
                        postId));
    }

    private ShortPostEntity requireOwnedShortPost(
            UUID postId,
            UUID ownerId) {
        ShortPostEntity shortPost = requireShortPost(postId);
        postSvc.requireOwnedPost(shortPost.getPost(), ownerId);
        return shortPost;
    }

    private OwnerShortPostResult toOwnerResult(
            ShortPostEntity shortPost,
            UUID ownerId) {
        return shortPostMapper.toOwnerResult(
                shortPost,
                profileSvc.requireProfile(ownerId),
                requireContentAttachment(shortPost.getPostId()));
    }

    private MediaEntity requireAllowedOwnedMedia(
            UUID mediaId,
            UUID ownerId) {
        return shortPostPolicy.requireAllowedMedia(
                mediaSvc.requireOwnedActiveMedia(mediaId, ownerId));
    }

    @Override
    public PostMediaEntity requireContentAttachment(UUID postId) {
        return requireContentAttachment(
                postId,
                postMediaSvc.findAttachments(postId));
    }

    private PostMediaEntity requireContentAttachment(
            UUID postId,
            List<PostMediaEntity> attachments) {
        if (attachments.size() != 1
                || attachments.getFirst().getRole()
                != PostMediaRole.CONTENT) {
            throw ExceptionFactory.invalidParam(
                    "error.short.mediaRequired",
                    postId);
        }

        return attachments.getFirst();
    }

    private Map<UUID, UserInfoEntity> loadProfilesByAuthorId(
            List<ShortPostEntity> shortPosts) {
        List<UUID> authorIds = shortPosts.stream()
                .map(shortPost -> shortPost.getPost().getAuthor().getId())
                .distinct()
                .toList();

        return profileSvc.findProfiles(authorIds).stream()
                .collect(Collectors.toMap(
                        profile -> profile.getUserId(),
                        profile -> profile));
    }

    private Map<UUID, List<PostMediaEntity>> loadAttachmentsByPostId(
            List<ShortPostEntity> shortPosts) {
        List<UUID> postIds = shortPosts.stream()
                .map(shortPost -> shortPost.getPostId())
                .toList();

        return postMediaSvc.findAttachments(
                        postIds,
                        PostMediaRole.CONTENT).stream()
                .collect(Collectors.groupingBy(
                        attachment -> attachment.getPost().getId()));
    }

    private UserInfoEntity requireLoadedProfile(
            Map<UUID, UserInfoEntity> profilesByAuthorId,
            UUID authorId) {
        UserInfoEntity profile = profilesByAuthorId.get(authorId);
        if (profile == null) {
            throw ExceptionFactory.notFound(
                    "error.profile.notFound",
                    authorId);
        }
        return profile;
    }
}
