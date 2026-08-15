package com.app.features.post.videopost.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.app.features.post.entity.PostEntity_;
import com.app.features.post.videopost.entity.VideoPostEntity;
import com.app.features.post.videopost.entity.VideoPostEntity_;

public interface VideoPostRepository
        extends JpaRepository<VideoPostEntity, UUID>,
        JpaSpecificationExecutor<VideoPostEntity> {

    @Override
    @EntityGraph(attributePaths = {
            VideoPostEntity_.POST,
            VideoPostEntity_.POST + "." + PostEntity_.AUTHOR
    })
    Page<VideoPostEntity> findAll(
            Specification<VideoPostEntity> specification,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            VideoPostEntity_.POST,
            VideoPostEntity_.POST + "." + PostEntity_.AUTHOR,
            VideoPostEntity_.POST + "." + PostEntity_.MODERATED_BY
    })
    Optional<VideoPostEntity> findDetailByPostId(UUID postId);

    @EntityGraph(attributePaths = {
            VideoPostEntity_.POST,
            VideoPostEntity_.POST + "." + PostEntity_.AUTHOR
    })
    List<VideoPostEntity> findAllByPostIdIn(Collection<UUID> postIds);
}
