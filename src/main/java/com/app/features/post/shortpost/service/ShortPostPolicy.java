package com.app.features.post.shortpost.service;

import com.app.features.media.entity.MediaEntity;

import jakarta.validation.constraints.NotNull;

public interface ShortPostPolicy {

    MediaEntity requireAllowedMedia(@NotNull MediaEntity media);
}
