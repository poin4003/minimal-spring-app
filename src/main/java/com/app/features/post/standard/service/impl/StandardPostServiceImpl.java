package com.app.features.post.standard.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.schema.result.PublicMediaResult;
import com.app.features.media.service.MediaService;
import com.app.features.media.support.MediaUrlResolver;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.enums.PostMediaRole;
import com.app.features.post.enums.PostType;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.schema.payload.CreateStandardPostPayload;
import com.app.features.post.schema.result.PostMediaResult;
import com.app.features.post.service.PostMediaService;
import com.app.features.post.service.PostService;
import com.app.features.post.standard.entity.StandardPostEntity;
import com.app.features.post.standard.repository.StandardPostRepository;
import com.app.features.post.standard.schema.result.OwnerStandardPostResult;
import com.app.features.post.standard.schema.result.PublicStandardPostResult;
import com.app.features.post.standard.service.StandardPostService;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.schema.result.UserPublicResult;
import com.app.features.user.service.ProfileService;
import com.app.features.user.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class StandardPostServiceImpl implements StandardPostService {

    private final UserService userSvc;
    private final ProfileService profileSvc;
    private final PostService postSvc;
    private final StandardPostRepository standardPostRepo;
    private final MediaService mediaSvc;
    private final PostMediaService postMediaSvc;
    private final MediaUrlResolver mediaUrlResolver;
    private final ModelMapper mapper;

    @Override
    @Transactional
    public OwnerStandardPostResult createStandardPost(
            @NotNull UUID authorId, 
            @NotNull @Valid CreateStandardPostPayload payload) {
        UserBaseEntity author = userSvc.requireUser(authorId);
        UserInfoEntity authorInfo = profileSvc.requireProfile(authorId);

        List<MediaEntity> media = mediaSvc.requireOwnedActiveMedia(payload.getMediaIds(), authorId);

        Map<UUID, MediaEntity> mediaById = media.stream()
                .collect(Collectors.toMap(
                        item -> item.getId(),
                        item -> item));

        List<MediaEntity> orderedMedia = payload.getMediaIds().stream()
                .map(mediaId -> mediaById.get(mediaId))
                .toList();

        PostEntity post = postSvc.createPendingPost(author, PostType.STANDARD);

        StandardPostEntity standardPost = new StandardPostEntity();
        standardPost.setPost(post);
        standardPost.setContent(payload.getContent());
        standardPost = standardPostRepo.save(standardPost);

        List<PostMediaEntity> attachments = postMediaSvc.createAttachments(
                post, PostMediaRole.CONTENT, orderedMedia);

        return toOwnerResult(standardPost, authorInfo, attachments);
    }

    @Override
    public PublicStandardPostResult getPublishedPost(UUID postId) {
        StandardPostEntity standardPost = requireStandardPost(postId);

        PostEntity post = standardPost.getPost();

        if (post.getModerationStatus() != PostModerationStatus.PUBLISHED) {
            throw ExceptionFactory.notFound("error.post.notFound", postId);
        }

        UserInfoEntity authorInfo = profileSvc.requireProfile(post.getAuthor().getId());

        return toPublicResult(
                standardPost,
                authorInfo,
                postMediaSvc.findAttachments(postId, PostMediaRole.CONTENT));
    }

    @Override
    public OwnerStandardPostResult getOwnerPost(UUID postId, UUID ownerId) {
        StandardPostEntity standardPost = requireOwnedStandardPost(postId, ownerId);

        return toOwnerResult(
                standardPost,
                profileSvc.requireProfile(ownerId),
                postMediaSvc.findAttachments(postId, PostMediaRole.CONTENT));
    }

    @Override
    @Transactional
    public void deleteOwnedPost(UUID postId, UUID ownerId) {
        StandardPostEntity standardPost = requireOwnedStandardPost(postId, ownerId);

        postSvc.deletePost(standardPost.getPost());
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
            UserInfoEntity authorInfo,
            List<PostMediaEntity> attachments) {

        PostEntity post = standardPost.getPost();

        OwnerStandardPostResult result = mapper.map(post, OwnerStandardPostResult.class);

        result.setContent(standardPost.getContent());
        result.setAuthor(toAuthorResult(post.getAuthor(), authorInfo));
        result.setMedia(toPostMediaResult(attachments));

        return result;
    }

    private PublicStandardPostResult toPublicResult(
            StandardPostEntity standardPost,
            UserInfoEntity authorInfo,
            List<PostMediaEntity> attachments) {
        PostEntity post = standardPost.getPost();

        PublicStandardPostResult result = mapper.map(post, PublicStandardPostResult.class);

        result.setContent(standardPost.getContent());
        result.setAuthor(toAuthorResult(post.getAuthor(), authorInfo));
        result.setMedia(toPostMediaResult(attachments));

        return result;
    }

    private UserPublicResult toAuthorResult(
            UserBaseEntity author,
            UserInfoEntity authorInfo) {
        UserPublicResult result = mapper.map(authorInfo, UserPublicResult.class);

        result.setId(author.getId());

        if (authorInfo.getAvatarMedia() != null) {
            result.setAvatarUrl(mediaUrlResolver.resolvePreviewUrl(authorInfo.getAvatarMedia()));
        }

        return result;
    }

    private List<PostMediaResult> toPostMediaResult(List<PostMediaEntity> attachments) {
        return attachments.stream()
                .map(attachment -> {
                    PostMediaResult result = mapper.map(attachment, PostMediaResult.class);

                    result.setMedia(toPublicMediaResult(attachment.getMedia()));

                    return result;
                })
                .toList();
    }

    private PublicMediaResult toPublicMediaResult(MediaEntity media) {
        PublicMediaResult result = mapper.map(media, PublicMediaResult.class);

        result.setContentUrl(mediaUrlResolver.resolveContentUrl(media));
        result.setOriginalUrl(mediaUrlResolver.resolveOriginalUrl(media));
        result.setThumbnailUrl(mediaUrlResolver.resolveThumbnailUrl(media));

        return result;
    }
}
