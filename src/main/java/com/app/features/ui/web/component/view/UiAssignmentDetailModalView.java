package com.app.features.ui.web.component.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiAssignmentDetailModalView {

    private final String id;
    private final String title;
    private final List<UiAssignmentDetailMetaView> metadata;
    private final String assignedTitle;
    private final String assignedEmptyMessage;
    private final List<UiAssignmentDetailItemView> assignedItems;
    private final String availableTitle;
    private final String availableEmptyMessage;
    private final List<UiAssignmentDetailItemView> availableItems;
}
