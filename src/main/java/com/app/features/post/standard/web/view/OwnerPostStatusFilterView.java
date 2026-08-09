package com.app.features.post.standard.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerPostStatusFilterView {

    private final String label;
    private final String path;
    private final boolean active;
}
