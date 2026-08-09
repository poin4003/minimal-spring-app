package com.app.features.media.mapper;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.app.features.media.entity.MediaEntity;
import com.app.features.media.entity.MediaVariantEntity;
import com.app.features.media.schema.result.MediaDetailResult;
import com.app.features.media.schema.result.MediaResult;
import com.app.features.media.schema.result.MediaVariantResult;
import com.app.features.media.schema.result.PublicMediaResult;
import com.app.features.media.support.MediaUrlResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaResultMapper {

    private final ModelMapper mapper;
    private final MediaUrlResolver mediaUrlResolver;

    public MediaResult toResult(MediaEntity media) {
        MediaResult result = mapper.map(media, MediaResult.class);
        populateUrls(result, media);
        return result;
    }

    public PublicMediaResult toPublicResult(MediaEntity media) {
        PublicMediaResult result = mapper.map(media, PublicMediaResult.class);
        result.setContentUrl(mediaUrlResolver.resolveContentUrl(media));
        result.setOriginalUrl(mediaUrlResolver.resolveOriginalUrl(media));
        result.setThumbnailUrl(mediaUrlResolver.resolveThumbnailUrl(media));
        return result;
    }

    public MediaDetailResult toDetailResult(
            MediaEntity media,
            List<MediaVariantEntity> variants) {
        MediaDetailResult result = mapper.map(media, MediaDetailResult.class);
        result.setVariants(variants.stream()
                .map(variant -> mapper.map(variant, MediaVariantResult.class))
                .toList());
        populateUrls(result, media);
        return result;
    }

    private void populateUrls(
            MediaResult result,
            MediaEntity media) {
        result.setContentUrl(mediaUrlResolver.resolveContentUrl(media));
        result.setOriginalUrl(mediaUrlResolver.resolveOriginalUrl(media));
        result.setThumbnailUrl(mediaUrlResolver.resolveThumbnailUrl(media));
    }
}
