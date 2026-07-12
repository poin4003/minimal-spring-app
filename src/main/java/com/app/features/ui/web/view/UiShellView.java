package com.app.features.ui.web.view;

import java.util.List;

import com.app.core.menu.MenuItem;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiShellView {

    private final String title;
    private final String logoutPath;
    private final UiCurrentUserView currentUser;
    private final List<MenuItem> menuTree;
}
