package com.app.features.post.aimoderation.web.view;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostAiModerationPanelView {

    public static final String ATTRIBUTE = "aiModeration";

    private final String id;

    private final String updatePath;

    private final PostAiModerationConfigForm form;

    private final List<PostAiModerationModeOptionView> modes;

    @Builder.Default
    private final Map<String, String> fieldErrors = Map.of();

    private final String statusLabel;

    private final String availabilityLabel;

    private final String availabilityDescription;

    private final String availabilityBadgeClass;

    private final String updatedAt;

    private final boolean saved;
}
