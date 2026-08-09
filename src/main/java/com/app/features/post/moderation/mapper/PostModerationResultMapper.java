package com.app.features.post.moderation.mapper;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.app.features.media.mapper.MediaResultMapper;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.mapper.PostResultMapper;
import com.app.features.post.moderation.schema.result.ModerationPostMediaResult;
import com.app.features.post.moderation.schema.result.ModerationPostResult;
import com.app.features.post.moderation.schema.result.ModerationShortPostDetailResult;
import com.app.features.post.moderation.schema.result.ModerationPostStateResult;
import com.app.features.post.moderation.schema.result.ModerationStandardPostDetailResult;
import com.app.features.post.shortpost.entity.ShortPostEntity;
import com.app.features.post.standard.entity.StandardPostEntity;
import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.mapper.UserPublicResultMapper;
import com.app.features.user.schema.result.UserShortResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostModerationResultMapper {

    private final ModelMapper mapper;
    private final MediaResultMapper mediaResultMapper;
    private final UserPublicResultMapper userPublicResultMapper;
    private final PostResultMapper postResultMapper;

    public ModerationPostResult toListResult(
            PostEntity post,
            UserInfoEntity authorInfo) {
        ModerationPostResult result = mapper.map(
                post,
                ModerationPostResult.class);
        result.setAuthor(userPublicResultMapper.toResult(
                post.getAuthor(),
                authorInfo));
        return result;
    }

    public ModerationStandardPostDetailResult toStandardDetailResult(
            StandardPostEntity standardPost,
            UserInfoEntity authorInfo,
            List<PostMediaEntity> attachments) {
        PostEntity post = standardPost.getPost();
        ModerationStandardPostDetailResult result =
                new ModerationStandardPostDetailResult();
        result.setPost(postResultMapper.toSummary(post, authorInfo));
        result.setState(toState(post));
        result.setContent(standardPost.getContent());
        result.setMedia(toMediaResults(attachments));

        return result;
    }

    public ModerationShortPostDetailResult toShortDetailResult(
            ShortPostEntity shortPost,
            UserInfoEntity authorInfo,
            PostMediaEntity attachment) {
        PostEntity post = shortPost.getPost();
        ModerationShortPostDetailResult result =
                new ModerationShortPostDetailResult();
        result.setPost(postResultMapper.toSummary(post, authorInfo));
        result.setState(toState(post));
        result.setCaption(shortPost.getCaption());
        result.setMedia(toMediaResult(attachment));
        return result;
    }

    private ModerationPostStateResult toState(PostEntity post) {
        ModerationPostStateResult result = mapper.map(
                post,
                ModerationPostStateResult.class);

        if (post.getModeratedBy() != null) {
            result.setModeratedBy(mapper.map(
                    post.getModeratedBy(),
                    UserShortResult.class));
        }

        return result;
    }

    private List<ModerationPostMediaResult> toMediaResults(
            List<PostMediaEntity> attachments) {
        return attachments.stream()
                .map(attachment -> toMediaResult(attachment))
                .toList();
    }

    private ModerationPostMediaResult toMediaResult(
            PostMediaEntity attachment) {
        ModerationPostMediaResult result = mapper.map(
                attachment,
                ModerationPostMediaResult.class);
        result.setMedia(mediaResultMapper.toResult(
                attachment.getMedia()));
        return result;
    }
}
