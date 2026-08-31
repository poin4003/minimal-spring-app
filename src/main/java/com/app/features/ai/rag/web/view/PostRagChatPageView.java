package com.app.features.ai.rag.web.view;

import com.app.features.ui.web.view.SocialShellView;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostRagChatPageView {

    public static final String ATTRIBUTE = "page";

    private final String title;
    private final String askPath;
    private final String sessionStorageKey;
    private final SocialShellView shell;
}
