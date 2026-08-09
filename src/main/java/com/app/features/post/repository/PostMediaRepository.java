package com.app.features.post.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.app.features.media.entity.MediaEntity_;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.entity.PostMediaEntity_;
import com.app.features.post.enums.PostMediaRole;

public interface PostMediaRepository
        extends JpaRepository<PostMediaEntity, UUID>, JpaSpecificationExecutor<PostMediaEntity> {

    @EntityGraph(attributePaths = {
            PostMediaEntity_.MEDIA,
            PostMediaEntity_.MEDIA + "." + MediaEntity_.CREATED_BY })
    List<PostMediaEntity> findAllByPost_IdInOrderByPost_IdAscRoleAscPositionAsc(
            Collection<UUID> postIds);

    @EntityGraph(attributePaths = {
            PostMediaEntity_.MEDIA,
            PostMediaEntity_.MEDIA + "." + MediaEntity_.CREATED_BY })
    List<PostMediaEntity> findAllByPost_IdAndRoleOrderByPositionAsc(
            UUID postId,
            PostMediaRole role);

    @EntityGraph(attributePaths = {
            PostMediaEntity_.POST,
            PostMediaEntity_.MEDIA,
            PostMediaEntity_.MEDIA + "." + MediaEntity_.CREATED_BY })
    List<PostMediaEntity> findAllByPost_IdInAndRoleOrderByPost_IdAscPositionAsc(
            Collection<UUID> postIds,
            PostMediaRole role);

    boolean existsByMedia_Id(UUID mediaId);

    void deleteAllByPost_Id(UUID postId);
}
