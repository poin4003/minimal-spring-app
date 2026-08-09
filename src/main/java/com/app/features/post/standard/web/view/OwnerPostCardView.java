package com.app.features.post.standard.web.view;

import com.app.features.post.standard.schema.result.OwnerStandardPostResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerPostCardView {

    private final OwnerStandardPostResult post;
    private final String detailPath;
    private final String editPath;
    private final String statusLabel;
    private final String statusBadgeClass;
}
