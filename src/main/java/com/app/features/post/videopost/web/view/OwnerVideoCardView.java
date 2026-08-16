package com.app.features.post.videopost.web.view;

import java.util.List;

import com.app.features.post.videopost.schema.result.OwnerVideoPostResult;
import com.app.features.post.web.view.OwnerPostActionView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerVideoCardView {

    private final OwnerVideoPostResult video;
    private final String detailPath;
    private final String editPath;
    private final String statusLabel;
    private final String statusBadgeClass;
    private final List<OwnerPostActionView> actions;
    private final boolean editable;
}
