package com.app.features.post.videopost.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.app.features.media.mapper.MediaResultMapper;
import com.app.features.post.entity.PostMediaEntity;
import com.app.features.post.videopost.entity.VideoSeriesEntity;
import com.app.features.post.videopost.entity.VideoSeriesItemEntity;
import com.app.features.post.videopost.schema.result.VideoSeriesItemResult;
import com.app.features.post.videopost.schema.result.VideoSeriesResult;
import com.app.features.user.entity.UserInfoEntity;
import com.app.features.user.mapper.UserPublicResultMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VideoSeriesResultMapper {

    private final ModelMapper mapper;
    private final MediaResultMapper mediaResultMapper;
    private final UserPublicResultMapper userPublicResultMapper;
    private final VideoPostResultMapper videoPostResultMapper;

    public VideoSeriesResult toResult(
            VideoSeriesEntity series,
            UserInfoEntity ownerInfo) {
        VideoSeriesResult result = mapper.map(
                series,
                VideoSeriesResult.class);
        result.setOwner(userPublicResultMapper.toResult(
                series.getOwner(),
                ownerInfo));

        if (series.getCoverMedia() != null) {
            result.setCoverMedia(mediaResultMapper.toPublicResult(
                    series.getCoverMedia()));
        }

        return result;
    }

    public VideoSeriesItemResult toItemResult(
            VideoSeriesItemEntity item,
            PostMediaEntity content) {
        VideoSeriesItemResult result = mapper.map(
                item,
                VideoSeriesItemResult.class);
        result.setVideo(videoPostResultMapper.toSummaryResult(
                item.getVideoPost(),
                content));
        return result;
    }

}
