package com.app.features.cronjob.web.view;

import com.app.features.ui.web.component.view.UiMetadataModalView;
import com.app.features.ui.web.component.view.UiModalView;
import com.app.features.ui.web.component.view.UiTableView;
import com.app.features.ui.web.view.UiShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CronJobListPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final String heading;
    private final String description;
    private final UiShellView shell;
    private final UiTableView cronJobTable;
    private final UiMetadataModalView metadataModal;
    private final UiModalView detailModal;
    private final String errorMessage;
    private final boolean openMetadataModal;
    private final boolean openDetailModal;
}
