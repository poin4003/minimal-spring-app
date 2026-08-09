package com.app.features.ui.web.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialNavigationItemView {

    private final String label;
    private final String icon;
    private final String path;
    private final boolean active;
}
