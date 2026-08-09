package com.app.features.post.shortpost.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.app.config.settings.AppProperties;
import com.app.core.exception.ExceptionFactory;
import com.app.features.media.entity.MediaEntity;
import com.app.features.media.enums.MediaKind;
import com.app.features.post.shortpost.service.ShortPostPolicy;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class ShortPostPolicyImpl implements ShortPostPolicy {

    private final AppProperties appProperties;

    @Override
    public MediaEntity requireAllowedMedia(MediaEntity media) {
        AppProperties.ShortPostSettings settings =
                appProperties.getPost().getShortPost();

        if (!settings.getAllowedMediaKinds().contains(media.getKind())) {
            throw ExceptionFactory.invalidParam(
                    "error.short.mediaKindNotAllowed",
                    media.getKind());
        }

        if (media.getKind() == MediaKind.VIDEO
                || media.getKind() == MediaKind.AUDIO) {
            requireAllowedDuration(media, settings);
        }

        if (media.getKind() == MediaKind.VIDEO
                || media.getKind() == MediaKind.IMAGE) {
            requireAllowedDimensions(media, settings);
        }

        return media;
    }

    private void requireAllowedDuration(
            MediaEntity media,
            AppProperties.ShortPostSettings settings) {
        if (media.getDurationMillis() == null) {
            throw ExceptionFactory.invalidParam(
                    "error.short.durationUnavailable",
                    media.getId());
        }

        if (media.getDurationMillis() > settings.getMaxDuration().toMillis()) {
            throw ExceptionFactory.invalidParam(
                    "error.short.durationTooLong",
                    settings.getMaxDuration());
        }
    }

    private void requireAllowedDimensions(
            MediaEntity media,
            AppProperties.ShortPostSettings settings) {
        if (media.getOriginalWidth() == null
                || media.getOriginalHeight() == null) {
            throw ExceptionFactory.invalidParam(
                    "error.short.dimensionsUnavailable",
                    media.getId());
        }

        double aspectRatio = (double) media.getOriginalWidth()
                / media.getOriginalHeight();
        if (aspectRatio < settings.getMinAspectRatio()
                || aspectRatio > settings.getMaxAspectRatio()) {
            throw ExceptionFactory.invalidParam(
                    "error.short.aspectRatioNotAllowed",
                    settings.getMinAspectRatio(),
                    settings.getMaxAspectRatio());
        }

        int shortEdge = Math.min(
                media.getOriginalWidth(),
                media.getOriginalHeight());
        if (shortEdge < settings.getMinShortEdge()) {
            throw ExceptionFactory.invalidParam(
                    "error.short.resolutionTooLow",
                    settings.getMinShortEdge());
        }
    }
}
