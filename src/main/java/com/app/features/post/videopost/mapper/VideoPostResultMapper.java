package com.app.features.post.videopost.mapper;

import org.springframework.stereotype.Component;

import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.mapper.PostMediaResultMapper;
import com.app.features.post.mapper.PostResultMapper;
import com.app.features.post.videopost.entity.VideoPostEntity;
import com.app.features.post.videopost.schema.result.OwnerVideoPostResult;
import com.app.features.post.videopost.schema.result.PublicVideoPostResult;
import com.app.features.post.videopost.schema.result.VideoPostSummaryResult;
import com.app.features.user.entity.UserInfoEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VideoPostResultMapper {

    private final PostResultMapper postResultMapper;
    private final PostMediaResultMapper postMediaResultMapper;

    public OwnerVideoPostResult toOwnerResult(
            VideoPostEntity videoPost,
            UserInfoEntity authorInfo,
            PostMediaEntity content) {
        PostEntity post = videoPost.getPost();
        OwnerVideoPostResult result = new OwnerVideoPostResult();
        result.setPost(postResultMapper.toSummary(post, authorInfo));
        result.setState(postResultMapper.toOwnerState(post));
        result.setTitle(videoPost.getTitle());
        result.setDescription(videoPost.getDescription());
        result.setContent(postMediaResultMapper.toResult(content));
        return result;
    }

    public PublicVideoPostResult toPublicResult(
            VideoPostEntity videoPost,
            UserInfoEntity authorInfo,
            PostMediaEntity content) {
        PublicVideoPostResult result = new PublicVideoPostResult();
        result.setPost(postResultMapper.toSummary(
                videoPost.getPost(),
                authorInfo));
        result.setTitle(videoPost.getTitle());
        result.setDescription(videoPost.getDescription());
        result.setContent(postMediaResultMapper.toResult(content));
        return result;
    }

    public VideoPostSummaryResult toSummaryResult(
            VideoPostEntity videoPost,
            PostMediaEntity content) {
        PostEntity post = videoPost.getPost();
        VideoPostSummaryResult result = new VideoPostSummaryResult();
        result.setId(post.getId());
        result.setTitle(videoPost.getTitle());
        result.setPublishedAt(post.getPublishedAt());
        result.setContent(postMediaResultMapper.toResult(content));
        return result;
    }
}
