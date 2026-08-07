package com.app.features.post.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import com.app.features.media.entity.MediaEntity;
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
}
