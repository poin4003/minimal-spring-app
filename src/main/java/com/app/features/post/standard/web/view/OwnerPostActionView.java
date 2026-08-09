package com.app.features.post.standard.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerPostActionView {

    private final String label;
    private final String modalPath;
    private final String iconClass;
    private final String buttonClass;
}
