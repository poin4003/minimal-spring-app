package com.app.features.media.web.view;

import java.util.List;

import com.app.features.media.schema.filter.MediaFilterCriteria;
import com.app.features.ui.web.component.view.UiAssignmentPanelView;
import com.app.features.ui.web.component.view.UiMetadataItemView;
import com.app.features.ui.web.view.UiShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaThumbnailPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final String listPath;
    private final String backPath;
    private final UiShellView shell;
    private final MediaFilterCriteria filter;
    private final MediaUploadComponentView uploadComponent;
    private final List<UiMetadataItemView> metadataItems;
    private final UiAssignmentPanelView assignmentPanel;
}
