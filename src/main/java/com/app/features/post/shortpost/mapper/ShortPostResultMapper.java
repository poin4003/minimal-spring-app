package com.app.features.post.shortpost.mapper;

import org.springframework.stereotype.Component;

import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.mapper.PostMediaResultMapper;
import com.app.features.post.mapper.PostResultMapper;
import com.app.features.post.shortpost.entity.ShortPostEntity;
import com.app.features.post.shortpost.schema.result.OwnerShortPostResult;
import com.app.features.post.shortpost.schema.result.PublicShortPostResult;
import com.app.features.user.entity.UserInfoEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShortPostResultMapper {

    private final PostResultMapper postResultMapper;
    private final PostMediaResultMapper postMediaResultMapper;

    public OwnerShortPostResult toOwnerResult(
            ShortPostEntity shortPost,
            UserInfoEntity authorInfo,
            PostMediaEntity attachment) {
        PostEntity post = shortPost.getPost();
        OwnerShortPostResult result = new OwnerShortPostResult();
        result.setPost(postResultMapper.toSummary(post, authorInfo));
        result.setState(postResultMapper.toOwnerState(post));
        result.setCaption(shortPost.getCaption());
        result.setMedia(postMediaResultMapper.toResult(attachment));
        return result;
    }

    public PublicShortPostResult toPublicResult(
            ShortPostEntity shortPost,
            UserInfoEntity authorInfo,
            PostMediaEntity attachment) {
        PublicShortPostResult result = new PublicShortPostResult();
        result.setPost(postResultMapper.toSummary(
                shortPost.getPost(),
                authorInfo));
        result.setCaption(shortPost.getCaption());
        result.setMedia(postMediaResultMapper.toResult(attachment));
        return result;
    }
}
