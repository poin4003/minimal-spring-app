package com.app.features.post.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.app.features.post.entity.PostEntity;
import com.app.features.post.schema.result.OwnerPostStateResult;
import com.app.features.post.schema.result.PostSummaryResult;
import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.mapper.UserPublicResultMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostResultMapper {

    private final ModelMapper mapper;
    private final UserPublicResultMapper userPublicResultMapper;

    public PostSummaryResult toSummary(
            PostEntity post,
            UserInfoEntity authorInfo) {
        PostSummaryResult result = mapper.map(
                post,
                PostSummaryResult.class);
        result.setAuthor(userPublicResultMapper.toResult(
                post.getAuthor(),
                authorInfo));
        return result;
    }

    public OwnerPostStateResult toOwnerState(PostEntity post) {
        return mapper.map(post, OwnerPostStateResult.class);
    }
}
