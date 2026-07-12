package com.app.features.ui.web.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiCurrentUserView {

    private final String email;
    private final List<String> authorities;
}
