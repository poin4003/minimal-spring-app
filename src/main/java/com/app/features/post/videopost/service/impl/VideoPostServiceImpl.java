package com.app.features.post.videopost.service.impl;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.service.MediaService;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.enums.PostLifecycleStatus;
import com.app.features.post.enums.PostMediaRole;
import com.app.features.post.enums.PostType;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.service.PostMediaService;
import com.app.features.post.service.PostLifecycleService;
import com.app.features.post.service.PostService;
import com.app.features.post.videopost.entity.VideoPostEntity;
import com.app.features.post.videopost.mapper.VideoPostResultMapper;
import com.app.features.post.videopost.repository.VideoPostRepository;
import com.app.features.post.videopost.repository.spec.VideoPostSpecification;
import com.app.features.post.videopost.schema.filter.OwnerVideoPostFilterCriteria;
import com.app.features.post.videopost.schema.filter.PublicVideoPostFilterCriteria;
import com.app.features.post.videopost.schema.payload.CreateVideoPostPayload;
import com.app.features.post.videopost.schema.payload.UpdateVideoPostPayload;
import com.app.features.post.videopost.schema.result.OwnerVideoPostResult;
import com.app.features.post.videopost.schema.result.PublicVideoPostResult;
import com.app.features.post.videopost.service.VideoPostService;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.service.ProfileService;
import com.app.features.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VideoPostServiceImpl implements VideoPostService {

    private final UserService userSvc;
    private final ProfileService profileSvc;
    private final PostService postSvc;
    private final PostLifecycleService postLifecycleSvc;
    private final VideoPostRepository videoPostRepo;
    private final MediaService mediaSvc;
    private final PostMediaService postMediaSvc;
    private final VideoPostResultMapper videoPostMapper;

    @Override
    @Transactional
    public OwnerVideoPostResult createVideoPost(
            UUID authorId,
            CreateVideoPostPayload payload) {
        UserBaseEntity author = userSvc.requireUser(authorId);
        VideoPostEntity videoPost = createDraftVideoPostInternal(
                author,
                payload);

        return videoPostMapper.toOwnerResult(
                videoPost,
                profileSvc.requireProfile(authorId),
                requireContentAttachment(videoPost.getPostId()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public VideoPostEntity createDraftVideoPost(
            UserBaseEntity author,
            CreateVideoPostPayload payload) {
        return createDraftVideoPostInternal(author, payload);
    }

    private VideoPostEntity createDraftVideoPostInternal(
            UserBaseEntity author,
            CreateVideoPostPayload payload) {
        MediaEntity content = requireOwnedVideoMedia(
                payload.getSourceMediaId(),
                author.getId());
        PostEntity post = postSvc.createDraftPost(author, PostType.VIDEO);

        VideoPostEntity videoPost = new VideoPostEntity();
        videoPost.setPost(post);
        videoPost.setTitle(payload.getTitle());
        videoPost.setDescription(payload.getDescription());
        videoPost = videoPostRepo.save(videoPost);

        postMediaSvc.createAttachments(
                post,
                PostMediaRole.CONTENT,
                List.of(content));

        return videoPost;
    }

    @Override
    @Transactional
    public OwnerVideoPostResult updateOwnedVideoPost(
            UUID postId,
            UUID ownerId,
            UpdateVideoPostPayload payload) {
        MediaEntity content = requireOwnedVideoMedia(
                payload.getSourceMediaId(),
                ownerId);
        PostEntity post = postSvc.prepareOwnedPostForUpdate(postId, ownerId);
        VideoPostEntity videoPost = requireVideoPost(postId);

        videoPost.setTitle(payload.getTitle());
        videoPost.setDescription(payload.getDescription());

        PostMediaEntity attachment = postMediaSvc.replaceAttachments(
                post,
                PostMediaRole.CONTENT,
                List.of(content)).getFirst();

        return videoPostMapper.toOwnerResult(
                videoPost,
                profileSvc.requireProfile(ownerId),
                attachment);
    }

    @Override
    @Transactional
    public void submitOwnedPostForReview(
            UUID postId,
            UUID ownerId) {
        PostEntity post = postSvc.requireOwnedPostForUpdate(postId, ownerId);
        requireVideoPost(postId);
        PostMediaEntity content = requireContentAttachment(
                postId,
                postMediaSvc.requirePublishableMedia(post));

        requireVideoMedia(content.getMedia());
        postSvc.submitForReview(post);
    }

    @Override
    @Transactional
    public void archiveOwnedPost(UUID postId, UUID ownerId) {
        postLifecycleSvc.archive(
                requireOwnedPostForLifecycle(postId, ownerId),
                LocalDateTime.now());
    }

    @Override
    @Transactional
    public void restoreArchivedOwnedPost(UUID postId, UUID ownerId) {
        postLifecycleSvc.restoreArchived(requireOwnedPostForLifecycle(
                postId,
                ownerId));
    }

    @Override
    @Transactional
    public void deleteOwnedPost(UUID postId, UUID ownerId) {
        postLifecycleSvc.softDelete(
                requireOwnedPostForLifecycle(postId, ownerId),
                LocalDateTime.now());
    }

    @Override
    @Transactional
    public void restoreDeletedOwnedPost(UUID postId, UUID ownerId) {
        postLifecycleSvc.restoreDeleted(requireOwnedPostForLifecycle(
                postId,
                ownerId));
    }

    @Override
    public PublicVideoPostResult getPublishedPost(UUID postId) {
        VideoPostEntity videoPost = requireVideoPost(postId);
        PostEntity post = videoPost.getPost();

        if (post.getLifecycleStatus() != PostLifecycleStatus.ACTIVE
                || post.getModerationStatus()
                != PostModerationStatus.PUBLISHED) {
            throw ExceptionFactory.notFound("error.post.notFound", postId);
        }

        return videoPostMapper.toPublicResult(
                videoPost,
                profileSvc.requireProfile(post.getAuthor().getId()),
                requireContentAttachment(postId));
    }

    @Override
    public OwnerVideoPostResult getOwnerPost(
            UUID postId,
            UUID ownerId) {
        VideoPostEntity videoPost = requireOwnedVideoPost(postId, ownerId);

        return videoPostMapper.toOwnerResult(
                videoPost,
                profileSvc.requireProfile(ownerId),
                requireContentAttachment(postId));
    }

    @Override
    public Page<PublicVideoPostResult> getPublishedPosts(
            PublicVideoPostFilterCriteria criteria,
            Pageable pageable) {
        Page<VideoPostEntity> entityPage = videoPostRepo.findAll(
                VideoPostSpecification.published(criteria),
                pageable);
        Map<UUID, UserInfoEntity> profilesByAuthorId = profileSvc
                .requireProfiles(entityPage.getContent().stream()
                        .map(videoPost -> videoPost
                                .getPost()
                                .getAuthor()
                                .getId())
                        .toList());
        Map<UUID, PostMediaEntity> contentByPostId = loadContentByPostId(
                entityPage.getContent());

        return entityPage.map(videoPost -> {
            PostEntity post = videoPost.getPost();
            return videoPostMapper.toPublicResult(
                    videoPost,
                    profileSvc.requireProfile(
                            profilesByAuthorId,
                            post.getAuthor().getId()),
                    contentByPostId.get(post.getId()));
        });
    }

    @Override
    public Page<OwnerVideoPostResult> getOwnedPosts(
            UUID ownerId,
            OwnerVideoPostFilterCriteria criteria,
            Pageable pageable) {
        Page<VideoPostEntity> entityPage = videoPostRepo.findAll(
                VideoPostSpecification.ownedBy(ownerId, criteria),
                pageable);
        Map<UUID, UserInfoEntity> profilesByAuthorId = profileSvc
                .requireProfiles(entityPage.getContent().stream()
                        .map(videoPost -> videoPost
                                .getPost()
                                .getAuthor()
                                .getId())
                        .toList());
        Map<UUID, PostMediaEntity> contentByPostId = loadContentByPostId(
                entityPage.getContent());

        return entityPage.map(videoPost -> {
            PostEntity post = videoPost.getPost();
            return videoPostMapper.toOwnerResult(
                    videoPost,
                    profileSvc.requireProfile(
                            profilesByAuthorId,
                            post.getAuthor().getId()),
                    contentByPostId.get(post.getId()));
        });
    }

    @Override
    public VideoPostEntity requireVideoPost(UUID postId) {
        return videoPostRepo.findDetailByPostId(postId)
                .orElseThrow(() -> ExceptionFactory.notFound(
                        "error.post.notFound",
                        postId));
    }

    @Override
    public List<VideoPostEntity> requireOwnedVideoPosts(
            Collection<UUID> postIds,
            UUID ownerId) {
        if (postIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<UUID> distinctIds = new LinkedHashSet<>(postIds);
        if (distinctIds.size() != postIds.size()) {
            throw ExceptionFactory.invalidParam(
                    "error.videoSeries.duplicateVideoIds");
        }

        List<VideoPostEntity> videoPosts = videoPostRepo.findAllByPostIdIn(
                distinctIds);
        if (videoPosts.size() != distinctIds.size()
                || videoPosts.stream().anyMatch(videoPost -> !videoPost
                        .getPost()
                        .getAuthor()
                        .getId()
                        .equals(ownerId))) {
            throw ExceptionFactory.invalidParam(
                    "error.videoSeries.videoSelectionInvalid");
        }

        Map<UUID, VideoPostEntity> videoPostById = videoPosts.stream()
                .collect(Collectors.toMap(
                        videoPost -> videoPost.getPostId(),
                        videoPost -> videoPost));

        return distinctIds.stream()
                .map(postId -> videoPostById.get(postId))
                .toList();
    }

    private VideoPostEntity requireOwnedVideoPost(
            UUID postId,
            UUID ownerId) {
        VideoPostEntity videoPost = requireVideoPost(postId);
        postSvc.requireOwnedPost(videoPost.getPost(), ownerId);
        return videoPost;
    }

    private PostEntity requireOwnedPostForLifecycle(
            UUID postId,
            UUID ownerId) {
        PostEntity post = postSvc.requireOwnedPostForUpdate(postId, ownerId);
        if (post.getType() != PostType.VIDEO) {
            throw ExceptionFactory.notFound("error.post.notFound", postId);
        }
        return post;
    }

    private MediaEntity requireOwnedVideoMedia(
            UUID mediaId,
            UUID ownerId) {
        return requireVideoMedia(
                mediaSvc.requireOwnedActiveMedia(mediaId, ownerId));
    }

    private MediaEntity requireVideoMedia(MediaEntity media) {
        if (media.getKind() != MediaKind.VIDEO) {
            throw ExceptionFactory.invalidParam(
                    "error.video.mediaKindNotAllowed",
                    media.getKind());
        }
        return media;
    }

    @Override
    public PostMediaEntity requireContentAttachment(UUID postId) {
        return requireContentAttachment(
                postId,
                postMediaSvc.findAttachments(
                        postId,
                        PostMediaRole.CONTENT));
    }

    @Override
    public Map<UUID, PostMediaEntity> requireContentAttachments(
            Collection<UUID> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        LinkedHashSet<UUID> distinctIds = new LinkedHashSet<>(postIds);
        Map<UUID, List<PostMediaEntity>> attachmentsByPostId = postMediaSvc
                .findAttachmentsByPostId(
                        distinctIds,
                        PostMediaRole.CONTENT);
        Map<UUID, PostMediaEntity> contentByPostId = new LinkedHashMap<>();

        distinctIds.forEach(postId -> contentByPostId.put(
                postId,
                requireContentAttachment(
                        postId,
                        attachmentsByPostId.getOrDefault(
                                postId,
                                List.of()))));

        return contentByPostId;
    }

    private PostMediaEntity requireContentAttachment(
            UUID postId,
            List<PostMediaEntity> attachments) {
        if (attachments.size() != 1
                || attachments.getFirst().getRole()
                != PostMediaRole.CONTENT) {
            throw ExceptionFactory.invalidParam(
                    "error.video.contentRequired",
                    postId);
        }
        return attachments.getFirst();
    }

    private Map<UUID, PostMediaEntity> loadContentByPostId(
            List<VideoPostEntity> videoPosts) {
        List<UUID> postIds = videoPosts.stream()
                .map(videoPost -> videoPost.getPostId())
                .toList();

        return requireContentAttachments(postIds);
    }

}
