package com.app.features.post.standard.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.app.features.post.mapper.PostMediaResultMapper;
import com.app.features.post.mapper.PostResultMapper;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.standard.entity.StandardPostEntity;
import com.app.features.post.standard.schema.result.OwnerStandardPostResult;
import com.app.features.post.standard.schema.result.PublicStandardPostResult;
import com.app.features.user.entity.UserInfoEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StandardPostResultMapper {

    private final PostResultMapper postResultMapper;
    private final PostMediaResultMapper postMediaResultMapper;

    public OwnerStandardPostResult toOwnerResult(
            StandardPostEntity standardPost,
            UserInfoEntity authorInfo,
            List<PostMediaEntity> attachments) {
        PostEntity post = standardPost.getPost();
        OwnerStandardPostResult result = new OwnerStandardPostResult();
        result.setPost(postResultMapper.toSummary(post, authorInfo));
        result.setState(postResultMapper.toOwnerState(post));
        result.setContent(standardPost.getContent());
        result.setMedia(postMediaResultMapper.toResults(attachments));
        return result;
    }

    public PublicStandardPostResult toPublicResult(
            StandardPostEntity standardPost,
            UserInfoEntity authorInfo,
            List<PostMediaEntity> attachments) {
        PostEntity post = standardPost.getPost();
        PublicStandardPostResult result = new PublicStandardPostResult();
        result.setPost(postResultMapper.toSummary(post, authorInfo));
        result.setContent(standardPost.getContent());
        result.setMedia(postMediaResultMapper.toResults(attachments));
        return result;
    }
}
