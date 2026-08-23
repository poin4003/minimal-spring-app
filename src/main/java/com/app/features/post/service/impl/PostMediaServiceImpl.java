package com.app.features.post.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.enums.RecordStatus;
import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.enums.PostMediaRole;
import com.app.features.post.repository.PostMediaRepository;
import com.app.features.post.service.PostMediaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostMediaServiceImpl implements PostMediaService {

    private final PostMediaRepository postMediaRepo;

    @Override
    public List<PostMediaEntity> findAttachments(UUID postId) {
        return postMediaRepo
                .findAllByPost_IdInOrderByPost_IdAscRoleAscPositionAsc(
                        List.of(postId));
    }

    @Override
    public List<PostMediaEntity> findAttachments(
            UUID postId,
            PostMediaRole role) {
        return postMediaRepo
                .findAllByPost_IdAndRoleOrderByPositionAsc(
                        postId,
                        role);
    }

    @Override
    public List<PostMediaEntity> findAttachments(
            Collection<UUID> postIds,
            PostMediaRole role) {
        if (postIds.isEmpty()) {
            return List.of();
        }

        return postMediaRepo.findAllByPost_IdInAndRoleOrderByPost_IdAscPositionAsc(
                postIds,
                role);
    }

    @Override
    public Map<UUID, List<PostMediaEntity>> findAttachmentsByPostId(
            Collection<UUID> postIds,
            PostMediaRole role) {
        return findAttachments(postIds, role).stream()
                .collect(Collectors.groupingBy(
                        attachment -> attachment.getPost().getId()));
    }

    @Override
    public List<PostMediaEntity> requirePublishableMedia(PostEntity post) {
        List<PostMediaEntity> attachments = findAttachments(post.getId());

        boolean hasUnavailableMedia = attachments.stream()
                .map(attachment -> attachment.getMedia())
                .anyMatch(media -> !media.getCreatedBy().getId().equals(post.getAuthor().getId())
                        || media.getStatus() != RecordStatus.ACTIVE
                        || media.getProcessingStatus() != MediaProcessingStatus.READY);

        if (hasUnavailableMedia) {
            throw ExceptionFactory.invalidParam(
                    "error.post.mediaNotReady",
                    post.getId());
        }

        return attachments;
    }

    @Override
    public boolean isMediaAttached(UUID mediaId) {
        return postMediaRepo.existsByMedia_Id(mediaId);
    }

    @Override
    public List<PostMediaEntity> createAttachments(
            PostEntity post, PostMediaRole role, List<MediaEntity> orderedMedia) {
        List<PostMediaEntity> attachments = IntStream.range(0, orderedMedia.size())
                .mapToObj(position -> {
                    PostMediaEntity attachment = new PostMediaEntity();
                    attachment.setPost(post);
                    attachment.setMedia(orderedMedia.get(position));
                    attachment.setRole(role);
                    attachment.setPosition(position);

                    return attachment;
                })
                .toList();

        return postMediaRepo.saveAll(attachments);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<PostMediaEntity> replaceAttachments(
            PostEntity post, PostMediaRole role, List<MediaEntity> orderedMedia) {
        List<PostMediaEntity> currentAttachments = findAttachments(post.getId(), role);

        postMediaRepo.deleteAll(currentAttachments);
        postMediaRepo.flush();

        return createAttachments(post, role, orderedMedia);
    }
}
