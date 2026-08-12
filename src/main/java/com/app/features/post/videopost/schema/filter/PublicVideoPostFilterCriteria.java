package com.app.features.post.videopost.schema.filter;

import java.util.UUID;

import lombok.Data;

@Data
public class PublicVideoPostFilterCriteria {

    private UUID authorId;

    private UUID seriesId;

    private String title;
}
