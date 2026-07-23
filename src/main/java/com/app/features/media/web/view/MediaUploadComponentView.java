package com.app.features.media.web.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaUploadComponentView {

    public static final String ATTRIBUTE = "upload";

    private final String id;
    private final String title;
    private final String description;
    private final String uploadPath;
    private final String accept;
    private final List<MediaUploadRuleView> rules;
}
