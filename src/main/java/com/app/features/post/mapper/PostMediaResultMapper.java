package com.app.features.post.mapper;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.app.features.media.mapper.MediaResultMapper;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.schema.result.PostMediaResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostMediaResultMapper {

    private final ModelMapper mapper;
    private final MediaResultMapper mediaResultMapper;

    public List<PostMediaResult> toResults(
            List<PostMediaEntity> attachments) {
        return attachments.stream()
                .map(attachment -> toResult(attachment))
                .toList();
    }

    public PostMediaResult toResult(PostMediaEntity attachment) {
        PostMediaResult result = mapper.map(
                attachment,
                PostMediaResult.class);
        result.setMedia(mediaResultMapper.toPublicResult(
                attachment.getMedia()));
        return result;
    }
}
