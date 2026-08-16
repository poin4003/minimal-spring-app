package com.app.features.post.moderation.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.core.constant.PermissionConstants;
import com.app.core.exception.ExceptionFactory;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.moderation.mapper.PostModerationResultMapper;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.moderation.repository.spec.PostModerationSpecification;
import com.app.features.post.moderation.schema.filter.ModerationPostFilterCriteria;
import com.app.features.post.moderation.schema.payload.RejectPostPayload;
import com.app.features.post.moderation.schema.result.ModerationPostResult;
import com.app.features.post.moderation.schema.result.ModerationPostDetailResult;
import com.app.features.post.moderation.schema.result.ModerationShortPostDetailResult;
import com.app.features.post.moderation.schema.result.ModerationStandardPostDetailResult;
import com.app.features.post.moderation.schema.result.ModerationVideoPostDetailResult;
import com.app.features.post.moderation.service.PostModerationService;
import com.app.features.post.repository.PostRepository;
import com.app.features.post.service.PostMediaService;
import com.app.features.post.service.PostService;
import com.app.features.post.shortpost.entity.ShortPostEntity;
import com.app.features.post.shortpost.service.ShortPostService;
import com.app.features.post.standard.entity.StandardPostEntity;
import com.app.features.post.standard.service.StandardPostService;
import com.app.features.post.videopost.entity.VideoPostEntity;
import com.app.features.post.videopost.service.VideoPostService;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.service.ProfileService;
import com.app.features.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@Secured(PermissionConstants.POST_MODERATE)
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostModerationServiceImpl implements PostModerationService {

    private final UserService userSvc;
    private final ProfileService profileSvc;
    private final PostService postSvc;
    private final PostMediaService postMediaSvc;
    private final StandardPostService standardPostSvc;
    private final ShortPostService shortPostSvc;
    private final VideoPostService videoPostSvc;
    private final PostRepository postRepo;
    private final PostModerationResultMapper postModerationMapper;

    @Override
    public Page<ModerationPostResult> getPendingPosts(
            ModerationPostFilterCriteria criteria,
            Pageable pageable) {
        Page<PostEntity> entityPage = postRepo.findAll(
                PostModerationSpecification.pendingReview(criteria),
                pageable);
        Map<UUID, UserInfoEntity> profilesByAuthorId = profileSvc
                .requireProfiles(entityPage.getContent().stream()
                        .map(post -> post.getAuthor().getId())
                        .toList());

        return entityPage.map(post -> postModerationMapper.toListResult(
                post,
                profileSvc.requireProfile(
                        profilesByAuthorId,
                        post.getAuthor().getId())));
    }

    @Override
    public ModerationStandardPostDetailResult getStandardPostDetail(UUID postId) {
        StandardPostEntity standardPost = standardPostSvc.requireStandardPost(postId);
        PostEntity post = postSvc.requirePendingPost(standardPost.getPost());
        UserInfoEntity authorInfo = profileSvc.requireProfile(post.getAuthor().getId());
        List<PostMediaEntity> attachments = postMediaSvc.findAttachments(postId);

        return postModerationMapper.toStandardDetailResult(
                standardPost,
                authorInfo,
                attachments);
    }

    @Override
    public ModerationPostDetailResult getPostDetail(UUID postId) {
        PostEntity post = postSvc.requirePendingPost(
                postRepo.findById(postId)
                        .orElseThrow(() -> ExceptionFactory.notFound(
                                "error.post.notFound",
                                postId)));

        return switch (post.getType()) {
            case STANDARD -> getStandardPostDetail(postId);
            case SHORT -> getShortPostDetail(postId);
            case VIDEO -> getVideoPostDetail(postId);
            default -> throw ExceptionFactory.invalidParam(
                    "error.post.typeUnsupported",
                    post.getType());
        };
    }

    @Override
    public ModerationShortPostDetailResult getShortPostDetail(UUID postId) {
        ShortPostEntity shortPost = shortPostSvc.requireShortPost(postId);
        PostEntity post = postSvc.requirePendingPost(shortPost.getPost());
        UserInfoEntity authorInfo = profileSvc.requireProfile(
                post.getAuthor().getId());

        return postModerationMapper.toShortDetailResult(
                shortPost,
                authorInfo,
                shortPostSvc.requireContentAttachment(postId));
    }

    @Override
    public ModerationVideoPostDetailResult getVideoPostDetail(UUID postId) {
        VideoPostEntity videoPost = videoPostSvc.requireVideoPost(postId);
        PostEntity post = postSvc.requirePendingPost(videoPost.getPost());
        UserInfoEntity authorInfo = profileSvc.requireProfile(
                post.getAuthor().getId());

        return postModerationMapper.toVideoDetailResult(
                videoPost,
                authorInfo,
                videoPostSvc.requireContentAttachment(postId));
    }

    @Override
    @Transactional
    public void publishPost(UUID postId, UUID moderatorId) {
        UserBaseEntity moderator = userSvc.requireUser(moderatorId);
        PostEntity post = postSvc.requirePendingPostForUpdate(postId);

        postMediaSvc.requirePublishableMedia(post);

        LocalDateTime moderatedAt = LocalDateTime.now();
        post.setModerationStatus(PostModerationStatus.PUBLISHED);
        post.setPublishedAt(moderatedAt);
        post.setModeratedBy(moderator);
        post.setModeratedAt(moderatedAt);
        post.setRejectionReason(null);
    }

    @Override
    @Transactional
    public void rejectPost(
            UUID postId,
            UUID moderatorId,
            RejectPostPayload payload) {
        UserBaseEntity moderator = userSvc.requireUser(moderatorId);
        PostEntity post = postSvc.requirePendingPostForUpdate(postId);

        post.setModerationStatus(PostModerationStatus.REJECTED);
        post.setPublishedAt(null);
        post.setModeratedBy(moderator);
        post.setModeratedAt(LocalDateTime.now());
        post.setRejectionReason(payload.getReason().trim());
    }

}
