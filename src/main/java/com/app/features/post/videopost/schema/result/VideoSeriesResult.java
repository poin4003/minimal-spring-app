package com.app.features.post.videopost.schema.result;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.features.media.schema.result.PublicMediaResult;
import com.app.features.user.schema.result.UserPublicResult;

import lombok.Data;

@Data
public class VideoSeriesResult {

    private UUID id;

    private UserPublicResult owner;

    private PublicMediaResult coverMedia;

    private String title;

    private String description;

    private int videoCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
