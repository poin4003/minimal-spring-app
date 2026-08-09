package com.app.features.post.shortpost.web.view;

import java.util.List;

import com.app.features.post.shortpost.schema.result.OwnerShortPostResult;
import com.app.features.post.web.view.OwnerPostActionView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerShortCardView {

    private final OwnerShortPostResult post;
    private final String detailPath;
    private final String editPath;
    private final String statusLabel;
    private final String statusBadgeClass;

    @Builder.Default
    private final List<OwnerPostActionView> actions = List.of();

    private final boolean editable;
}
