package com.app.features.post.standard.mapper;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.app.features.media.mapper.MediaResultMapper;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.schema.result.PostMediaResult;
import com.app.features.post.standard.entity.StandardPostEntity;
import com.app.features.post.standard.schema.result.OwnerStandardPostResult;
import com.app.features.post.standard.schema.result.PublicStandardPostResult;
import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.mapper.UserPublicResultMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StandardPostResultMapper {

    private final ModelMapper mapper;
    private final MediaResultMapper mediaResultMapper;
    private final UserPublicResultMapper userPublicResultMapper;

    public OwnerStandardPostResult toOwnerResult(
            StandardPostEntity standardPost,
            UserInfoEntity authorInfo,
            List<PostMediaEntity> attachments) {
        PostEntity post = standardPost.getPost();
        OwnerStandardPostResult result = mapper.map(
                post,
                OwnerStandardPostResult.class);
        result.setContent(standardPost.getContent());
        result.setAuthor(userPublicResultMapper.toResult(
                post.getAuthor(),
                authorInfo));
        result.setMedia(toPostMediaResults(attachments));
        return result;
    }

    public PublicStandardPostResult toPublicResult(
            StandardPostEntity standardPost,
            UserInfoEntity authorInfo,
            List<PostMediaEntity> attachments) {
        PostEntity post = standardPost.getPost();
        PublicStandardPostResult result = mapper.map(
                post,
                PublicStandardPostResult.class);
        result.setContent(standardPost.getContent());
        result.setAuthor(userPublicResultMapper.toResult(
                post.getAuthor(),
                authorInfo));
        result.setMedia(toPostMediaResults(attachments));
        return result;
    }

    private List<PostMediaResult> toPostMediaResults(
            List<PostMediaEntity> attachments) {
        return attachments.stream()
                .map(attachment -> {
                    PostMediaResult result = mapper.map(
                            attachment,
                            PostMediaResult.class);
                    result.setMedia(mediaResultMapper.toPublicResult(
                            attachment.getMedia()));
                    return result;
                })
                .toList();
    }
}
