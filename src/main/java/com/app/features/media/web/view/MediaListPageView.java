package com.app.features.media.web.view;

import java.util.List;

import com.app.core.enums.RecordStatus;
import com.app.core.schema.query.UiPageQuery;
import com.app.features.media.enums.MediaKind;
import com.app.features.media.enums.MediaProcessingStatus;
import com.app.features.media.schema.filter.MediaFilterCriteria;
import com.app.features.ui.web.component.view.UiConfirmModalView;
import com.app.features.ui.web.component.view.UiDetailModalView;
import com.app.features.ui.web.component.view.UiMetadataModalView;
import com.app.features.ui.web.view.UiShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MediaListPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final String listPath;
    private final UiShellView shell;
    private final MediaFilterCriteria filter;
    private final UiPageQuery query;
    private final List<MediaKind> kinds;
    private final List<MediaProcessingStatus> processingStatuses;
    private final List<RecordStatus> statuses;
    private final MediaGalleryView mediaGallery;
    private final MediaPreviewModalView previewModal;
    private final UiMetadataModalView metadataModal;
    private final UiDetailModalView detailModal;
    private final UiConfirmModalView deleteModal;
    private final UiConfirmModalView retryModal;
    private final boolean openPreviewModal;
    private final boolean openMetadataModal;
    private final boolean openDetailModal;
    private final boolean openDeleteModal;
    private final boolean openRetryModal;
}
